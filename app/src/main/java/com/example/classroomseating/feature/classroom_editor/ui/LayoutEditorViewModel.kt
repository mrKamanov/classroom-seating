package com.example.classroomseating.feature.classroom_editor.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classroomseating.domain.model.BoardPosition
import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.SeatingPlan
import com.example.classroomseating.domain.repository.ClassroomRepository
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LayoutEditorEvent {
    data object Saved : LayoutEditorEvent
}

/**
 * Конструктор кабинета (2.3): сетка парт, положение доски, генерация DeskEntity.
 */
@HiltViewModel
class LayoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val classroomRepository: ClassroomRepository,
    private val schoolClassRepository: SchoolClassRepository,
    private val seatingPlanRepository: SeatingPlanRepository
) : ViewModel() {

    val classId: String = checkNotNull(savedStateHandle["classId"])

    private val _name = MutableStateFlow("")
    private val _columns = MutableStateFlow(4)      // парт в ряду
    private val _rows = MutableStateFlow(3)          // рядов
    private val _boardPosition = MutableStateFlow(BoardPosition.TOP)
    private val _isSaving = MutableStateFlow(false)

    private val _events = MutableSharedFlow<LayoutEditorEvent>()
    val events: SharedFlow<LayoutEditorEvent> = _events.asSharedFlow()

    val name: StateFlow<String> = _name.asStateFlow()
    val columns: StateFlow<Int> = _columns.asStateFlow()
    val rows: StateFlow<Int> = _rows.asStateFlow()
    val boardPosition: StateFlow<BoardPosition> = _boardPosition.asStateFlow()
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var existingClassroomId: String? = null

    init {
        viewModelScope.launch {
            val plan = seatingPlanRepository.observeByClass(classId).first().firstOrNull()
            val existing = plan?.let { classroomRepository.getById(it.classroomId) }
            if (existing != null) {
                existingClassroomId = existing.id
                _name.value = existing.name
                _columns.value = existing.columnsCount
                _rows.value = existing.rowsCount
                _boardPosition.value = existing.boardPosition
            }
        }
    }

    fun setColumns(value: Int) {
        _columns.value = value.coerceIn(1, 8)
    }

    fun setRows(value: Int) {
        _rows.value = value.coerceIn(1, 5)
    }

    fun setBoardPosition(value: BoardPosition) {
        _boardPosition.value = value
    }

    fun updateName(value: String) {
        _name.value = value
    }

    fun save() {
        viewModelScope.launch {
            _isSaving.value = true
            val displayName = schoolClassRepository.observeById(classId).first()?.name ?: ""
            val classroomName = _name.value.trim().ifBlank { displayName.ifBlank { "Кабинет" } }

            val classroom = Classroom(
                id = existingClassroomId ?: UUID.randomUUID().toString(),
                name = classroomName,
                columnsCount = _columns.value,
                rowsCount = _rows.value,
                boardPosition = _boardPosition.value
            )
            val classroomId = classroomRepository.upsert(classroom)
            classroomRepository.replaceDesks(classroomId, buildDesks(classroomId))

            val plans = seatingPlanRepository.observeByClass(classId).first()
            if (plans.isEmpty()) {
                seatingPlanRepository.upsertPlan(
                    SeatingPlan(
                        id = UUID.randomUUID().toString(),
                        classId = classId,
                        classroomId = classroomId,
                        title = "$classroomName — рассадка"
                    )
                )
            }

            _isSaving.value = false
            _events.emit(LayoutEditorEvent.Saved)
        }
    }

    private fun buildDesks(classroomId: String): List<Desk> =
        buildList {
            // gridX = номер ряда (вдоль доски), gridY = номер парты в ряду (от доски).
            for (x in 0 until _rows.value) {
                for (y in 0 until _columns.value) {
                    add(
                        Desk(
                            id = UUID.randomUUID().toString(),
                            classroomId = classroomId,
                            gridX = x,
                            gridY = y
                        )
                    )
                }
            }
        }
}