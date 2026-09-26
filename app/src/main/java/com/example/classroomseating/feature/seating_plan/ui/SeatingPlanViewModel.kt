package com.example.classroomseating.feature.seating_plan.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classroomseating.core.util.AutoSeatPlanner
import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.SeatingAssignment
import com.example.classroomseating.domain.model.SeatingPlan
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student
import com.example.classroomseating.domain.repository.ClassroomRepository
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import com.example.classroomseating.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SeatKey(
    val deskId: String,
    val seatIndex: Int
)

data class SeatingPlanUiState(
    val plan: SeatingPlan? = null,
    val className: String? = null,
    val classroom: Classroom? = null,
    val desks: List<Desk> = emptyList(),
    val seatOccupants: Map<SeatKey, Student> = emptyMap(),
    val allStudents: List<Student> = emptyList(),
    val unassignedStudents: List<Student> = emptyList(),
    val restrictionViolations: Map<SeatKey, SeatingRestriction> = emptyMap()
) {
    val hasPlan: Boolean get() = plan != null
    val seatsCount: Int get() = desks.sumOf { it.capacity.coerceAtLeast(1) }
    val assignedCount: Int get() = seatOccupants.size
}

/**
 * Интерактивная рассадка с Drag-and-Drop (3.3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeatingPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seatingPlanRepository: SeatingPlanRepository,
    private val classroomRepository: ClassroomRepository,
    private val studentRepository: StudentRepository,
    schoolClassRepository: SchoolClassRepository
) : ViewModel() {

    val classId: String = checkNotNull(savedStateHandle["classId"])

    val uiState: StateFlow<SeatingPlanUiState> = seatingPlanRepository.observeByClass(classId)
        .map { plans -> plans.firstOrNull() }
        .flatMapLatest { plan ->
            if (plan == null) {
                flowOf(SeatingPlanUiState(plan = null))
            } else {
                combine(
                    classroomRepository.observeById(plan.classroomId),
                    classroomRepository.observeDesks(plan.classroomId),
                    seatingPlanRepository.observeAssignments(plan.id),
                    studentRepository.observeByClass(classId),
                    schoolClassRepository.observeById(classId)
                ) { classroom, desks, assignments, students, schoolClass ->
                    buildState(plan, classroom, desks, assignments, students, schoolClass?.name)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SeatingPlanUiState()
        )

    private fun buildState(
        plan: SeatingPlan,
        classroom: Classroom?,
        desks: List<Desk>,
        assignments: List<SeatingAssignment>,
        students: List<Student>,
        className: String?
    ): SeatingPlanUiState {
        val studentsById = students.associateBy { it.id }
        val seatOccupants = HashMap<SeatKey, Student>()
        assignments.forEach { assignment ->
            studentsById[assignment.studentId]?.let { student ->
                seatOccupants[SeatKey(assignment.deskId, assignment.seatIndex)] = student
            }
        }
        val assignedIds = assignments.map { it.studentId }.toSet()

        val rows = classroom?.rowsCount ?: 1
        val columns = classroom?.columnsCount ?: 1
        val desksById = desks.associateBy { it.id }
        val violations = HashMap<SeatKey, SeatingRestriction>()
        seatOccupants.forEach { (seat, student) ->
            val restriction = student.restriction
            val desk = desksById[seat.deskId]
            if (restriction.hasRestriction && desk != null &&
                !restriction.isSuitableAt(desk.gridX, desk.gridY, rows, columns)
            ) {
                violations[seat] = restriction
            }
        }

        return SeatingPlanUiState(
            plan = plan,
            className = className,
            classroom = classroom,
            desks = desks,
            seatOccupants = seatOccupants,
            allStudents = students,
            unassignedStudents = students.filter { it.id !in assignedIds },
            restrictionViolations = violations
        )
    }

    /**
     * Посадка ученика на место (3.3).
     * Место занято другим учеником -> автообмен (Swap); место свободно -> посадка/перенос.
     */
    fun onStudentDropped(studentId: String, deskId: String, seatIndex: Int) {
        viewModelScope.launch {
            val state = uiState.value
            val planId = state.plan?.id ?: return@launch
            val assignments = seatingPlanRepository.getAssignments(planId)
            val occupant = assignments.firstOrNull {
                it.deskId == deskId && it.seatIndex == seatIndex
            }
            val droppingStudent = assignments.firstOrNull { it.studentId == studentId }

            when {
                occupant == null -> seatingPlanRepository.assignStudent(planId, studentId, deskId, seatIndex)
                occupant.studentId == studentId -> Unit // уже на этом месте
                droppingStudent == null -> {
                    // Нерассаженный ученик кладётся на занятое место: прежний возвращается в список.
                    seatingPlanRepository.unassignStudent(planId, occupant.studentId)
                    seatingPlanRepository.assignStudent(planId, studentId, deskId, seatIndex)
                }
                else -> seatingPlanRepository.swapStudents(planId, droppingStudent, occupant)
            }
        }
    }

    /** Снять ученика с парты (возврат в список нерассаженных). */
    fun unassignStudent(studentId: String) {
        viewModelScope.launch {
            val planId = uiState.value.plan?.id ?: return@launch
            seatingPlanRepository.unassignStudent(planId, studentId)
        }
    }

    /** Автоматическая рассадка с учётом ограничений по здоровью. */
    fun autoArrange() {
        val state = uiState.value
        applyPlan(AutoSeatPlanner.arrange(state.allStudents, state.desks))
    }

    /** «Бросок кубика»: новая автовариант рассадки, категории здоровья сохранены. */
    fun shuffleArrange() {
        applyPlan(AutoSeatPlanner.arrange(uiState.value.allStudents, uiState.value.desks, Random.Default))
    }

    private fun applyPlan(entries: List<AutoSeatPlanner.SeatPlanEntry>) {
        viewModelScope.launch {
            val planId = uiState.value.plan?.id ?: return@launch
            seatingPlanRepository.clearAssignments(planId)
            entries.forEach { entry ->
                seatingPlanRepository.assignStudent(
                    planId = planId,
                    studentId = entry.studentId,
                    deskId = entry.deskId,
                    seatIndex = entry.seatIndex
                )
            }
        }
    }
}