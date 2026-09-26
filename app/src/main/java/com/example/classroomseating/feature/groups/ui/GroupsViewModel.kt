package com.example.classroomseating.feature.groups.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classroomseating.core.util.DeskCoord
import com.example.classroomseating.core.util.StudentGrouper
import com.example.classroomseating.core.util.buildDeskGrid
import com.example.classroomseating.domain.model.BoardPosition
import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.SeatingAssignment
import com.example.classroomseating.domain.model.SeatingPlan
import com.example.classroomseating.domain.model.Student
import com.example.classroomseating.domain.repository.ClassroomRepository
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import com.example.classroomseating.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SeatKey(
    val deskId: String,
    val seatIndex: Int
)

data class GroupUi(
    val index: Int,
    val students: List<Student>
)

data class GroupsSettings(
    val scenario: StudentGrouper.Scenario = StudentGrouper.Scenario.TERRITORIAL,
    val sizeMode: StudentGrouper.SizeMode = StudentGrouper.SizeMode.BY_COUNT,
    val groupCount: Int = 4,
    val sizePerGroup: Int = 4,
    val template: StudentGrouper.Template = StudentGrouper.Template.BANDS,
    val randomSeed: Long = 1L,
    val revision: Long = 0L
)

data class GroupsUiState(
    val plan: SeatingPlan? = null,
    val className: String? = null,
    val classroom: Classroom? = null,
    val desks: List<Desk> = emptyList(),
    val seatOccupants: Map<SeatKey, Student> = emptyMap(),
    val students: List<Student> = emptyList(),
    val groups: List<GroupUi> = emptyList(),
    val seatGroupMap: Map<SeatKey, Int> = emptyMap(),
    val deskGroup: Map<String, Int> = emptyMap(),
    /** Для мини-карты: номер группы для каждой координаты сетки (включая пустые парты). */
    val previewZones: Map<DeskCoord, Int> = emptyMap(),
    val ungrouped: List<Student> = emptyList(),
    val settings: GroupsSettings = GroupsSettings()
) {
    val hasPlan: Boolean get() = plan != null
}

/**
 * Деление класса на малые группы для групповой работы (только на экране,
 * без печати и сохранения). Доска всегда показывается снизу.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seatingPlanRepository: SeatingPlanRepository,
    private val classroomRepository: ClassroomRepository,
    private val studentRepository: StudentRepository,
    schoolClassRepository: SchoolClassRepository
) : ViewModel() {

    val classId: String = checkNotNull(savedStateHandle["classId"])

    private val settings = MutableStateFlow(GroupsSettings())

    /** Ручное распределение (studentId -> номер группы) для сценария MANUAL. */
    private val manualAssignments = HashMap<String, Int>()

    private data class GroupBase(
        val plan: SeatingPlan?,
        val className: String?,
        val classroom: Classroom?,
        val desks: List<Desk>,
        val assignments: List<SeatingAssignment>,
        val students: List<Student>
    )

    private val baseData: Flow<GroupBase> = seatingPlanRepository.observeByClass(classId)
        .map { plans -> plans.firstOrNull() }
        .flatMapLatest { plan ->
            if (plan == null) {
                flowOf(GroupBase(plan = null, className = null, classroom = null, desks = emptyList(), assignments = emptyList(), students = emptyList()))
            } else {
                combine(
                    classroomRepository.observeById(plan.classroomId),
                    classroomRepository.observeDesks(plan.classroomId),
                    seatingPlanRepository.observeAssignments(plan.id),
                    studentRepository.observeByClass(classId),
                    schoolClassRepository.observeById(classId)
                ) { classroom, desks, assignments, students, schoolClass ->
                    GroupBase(plan, schoolClass?.name, classroom, desks, assignments, students)
                }
            }
        }

    val uiState: StateFlow<GroupsUiState> = combine(baseData, settings) { base, s ->
        compute(base, s)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupsUiState()
    )

    fun setScenario(scenario: StudentGrouper.Scenario) {
        if (scenario == StudentGrouper.Scenario.MANUAL && manualAssignments.isEmpty()) {
            val students = uiState.value.students
            val chunks = StudentGrouper.chunk(StudentGrouper.alphabetical(students), effectiveCount(students.size))
            manualAssignments.clear()
            chunks.forEachIndexed { index, members ->
                members.forEach { manualAssignments[it.id] = index }
            }
        }
        bump { copy(scenario = scenario) }
    }

    fun setGroupCount(count: Int) {
        bump {
            copy(
                groupCount = count.coerceIn(1, 12),
                sizeMode = StudentGrouper.SizeMode.BY_COUNT
            )
        }
    }

    fun setSizeMode(mode: StudentGrouper.SizeMode) {
        bump { copy(sizeMode = mode) }
    }

    fun setSizePerGroup(delta: Int) {
        bump { copy(sizePerGroup = (sizePerGroup + delta).coerceIn(2, 12)) }
    }

    fun setTemplate(template: StudentGrouper.Template) {
        bump { copy(template = template) }
    }

    /** Перемешать заново в сценарии RANDOM. */
    fun reshuffle() {
        bump { copy(randomSeed = randomSeed + 1) }
    }

    /** Ручной режим: добавить первого нераспределённого ученика в группу [groupIndex]. */
    fun addToGroup(groupIndex: Int) {
        if (settings.value.scenario != StudentGrouper.Scenario.MANUAL) return
        val id = uiState.value.ungrouped.firstOrNull()?.id ?: return
        manualAssignments[id] = groupIndex
        bump { copy() }
    }

    /** Ручной режим: убрать последнего ученика группы [groupIndex] (в нераспределённые). */
    fun removeLastFromGroup(groupIndex: Int) {
        if (settings.value.scenario != StudentGrouper.Scenario.MANUAL) return
        val member = uiState.value.groups.firstOrNull { it.index == groupIndex }?.students?.lastOrNull() ?: return
        manualAssignments.remove(member.id)
        bump { copy() }
    }

    /** Ручной режим: посадить ранее не распределённого ученика в первую группу. */
    fun assignUngroupedToFirst(studentId: String) {
        if (settings.value.scenario != StudentGrouper.Scenario.MANUAL) return
        if (!manualAssignments.containsKey(studentId)) {
            manualAssignments[studentId] = 0
            bump { copy() }
        }
    }

    /** Ручной режим: убрать ученика из его группы (в нераспределённые). */
    fun removeFromGroup(studentId: String) {
        if (settings.value.scenario != StudentGrouper.Scenario.MANUAL) return
        if (manualAssignments.remove(studentId) != null) {
            bump { copy() }
        }
    }

    private fun bump(mutate: GroupsSettings.() -> GroupsSettings) {
        settings.value = settings.value.mutate().let { it.copy(revision = it.revision + 1) }
    }

    /** Сколько групп должно получиться при заданных настройках (для n учеников). */
    private fun effectiveCount(n: Int): Int {
        if (n <= 0) return 1
        return when (settings.value.sizeMode) {
            StudentGrouper.SizeMode.BY_COUNT -> settings.value.groupCount.coerceIn(1, n)
            StudentGrouper.SizeMode.PER_GROUP -> {
                val per = settings.value.sizePerGroup.coerceAtLeast(1)
                ((n + per - 1) / per).coerceIn(1, n)
            }
        }
    }

    private fun manualGroupCount(): Int =
        (manualAssignments.values.maxOrNull()?.plus(1)) ?: 0

    private fun compute(base: GroupBase, s: GroupsSettings): GroupsUiState {
        val plan = base.plan
        if (plan == null) return GroupsUiState(plan = null)

        val studentsById = base.students.associateBy { it.id }
        val seatOccupants = HashMap<SeatKey, Student>()
        base.assignments.forEach { assignment ->
            studentsById[assignment.studentId]?.let { student ->
                seatOccupants[SeatKey(assignment.deskId, assignment.seatIndex)] = student
            }
        }
        val desksById = base.desks.associateBy { it.id }
        val rows = base.classroom?.rowsCount ?: 1
        val columns = base.classroom?.columnsCount ?: 1

        val groupOf: HashMap<String, Int>
        val effectiveCount: Int
        val lines = buildDeskGrid(BoardPosition.BOTTOM, rows, columns).lines
        when (s.scenario) {
            StudentGrouper.Scenario.TERRITORIAL -> {
                val safeCount = effectiveCount(base.students.size)
                val desksByCoord = base.desks.associate { desk ->
                    DeskCoord(desk.gridX, desk.gridY) to
                        seatOccupants.count { it.key.deskId == desk.id }
                }
                val zones = StudentGrouper.partitionTerritorial(lines, desksByCoord, safeCount, s.template)
                groupOf = HashMap()
                seatOccupants.forEach { (key, student) ->
                    val desk = desksById[key.deskId] ?: return@forEach
                    zones[DeskCoord(desk.gridX, desk.gridY)]?.let { groupOf[student.id] = it }
                }
                // Реальное число зон, которые удалось образовать на сетке
                // (не больше, чем колонок/линий/ячеек) — без пустых «хвостовых» групп.
                effectiveCount = (zones.values.maxOrNull() ?: -1) + 1
            }

            StudentGrouper.Scenario.RANDOM -> {
                val safeCount = effectiveCount(base.students.size)
                val order = base.students.shuffled(Random(s.randomSeed))
                groupOf = HashMap()
                val chunks = StudentGrouper.chunk(order, safeCount)
                chunks.forEachIndexed { index, members ->
                    members.forEach { groupOf[it.id] = index }
                }
                effectiveCount = chunks.size
            }

            StudentGrouper.Scenario.ALPHABET -> {
                val safeCount = effectiveCount(base.students.size)
                val order = StudentGrouper.alphabetical(base.students)
                groupOf = HashMap()
                val chunks = StudentGrouper.chunk(order, safeCount)
                chunks.forEachIndexed { index, members ->
                    members.forEach { groupOf[it.id] = index }
                }
                effectiveCount = chunks.size
            }

            StudentGrouper.Scenario.MANUAL -> {
                if (manualAssignments.isEmpty()) {
                    StudentGrouper.chunk(StudentGrouper.alphabetical(base.students), effectiveCount(base.students.size))
                        .forEachIndexed { index, members -> members.forEach { manualAssignments[it.id] = index } }
                }
                groupOf = HashMap()
                manualAssignments
                    .filterKeys { it in studentsById }
                    .forEach { (studentId, index) -> groupOf[studentId] = index }
                effectiveCount = manualGroupCount()
            }
        }

        val seatGroupMap = HashMap<SeatKey, Int>()
        seatOccupants.forEach { (key, student) ->
            groupOf[student.id]?.let { seatGroupMap[key] = it }
        }

        val deskGroup = HashMap<String, Int>()
        seatGroupMap.forEach { (key, group) ->
            if (!deskGroup.containsKey(key.deskId)) deskGroup[key.deskId] = group
        }

        val groups = (0 until effectiveCount).map { index ->
            GroupUi(index = index, students = base.students.filter { groupOf[it.id] == index })
        }
        val ungrouped = base.students.filter { it.id !in groupOf }

        return GroupsUiState(
            plan = plan,
            className = base.className,
            classroom = base.classroom,
            desks = base.desks,
            seatOccupants = seatOccupants,
            students = base.students,
            groups = groups,
            seatGroupMap = seatGroupMap,
            deskGroup = deskGroup,
            ungrouped = ungrouped,
            settings = s
        )
    }
}