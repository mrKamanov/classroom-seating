package com.example.classroomseating.feature.class_management.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classroomseating.core.util.StudentDraft
import com.example.classroomseating.core.util.StudentFileParser
import com.example.classroomseating.domain.model.Gender
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Экран «Ученики класса» (2.2).
 */
@HiltViewModel
class StudentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val studentRepository: StudentRepository,
    schoolClassRepository: SchoolClassRepository
) : ViewModel() {

    val classId: String = checkNotNull(savedStateHandle["classId"])

    val className: StateFlow<String?> = schoolClassRepository.observeById(classId)
        .map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val students: StateFlow<List<Student>> = studentRepository.observeByClass(classId)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Предпросмотр учеников, найденных в выбранном Excel-файле. */
    data class FilePreview(val drafts: List<StudentDraft>)

    private val _filePreview = MutableStateFlow<FilePreview?>(null)
    val filePreview: StateFlow<FilePreview?> = _filePreview.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    /**
     * Сохранение ученика. При [student.id] == null создаётся новый.
     */
    fun saveStudent(student: Student) {
        viewModelScope.launch {
            val id = if (student.id.isBlank()) java.util.UUID.randomUUID().toString() else student.id
            studentRepository.upsert(student.copy(id = id, classId = classId))
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            studentRepository.delete(student.id)
        }
    }

    /**
     * Выбран файл Excel: читаем и разбираем ([journalFormat] = журнал «Моя школа»,
     * иначе шаблон приложения), результат показываем в предпросмотре.
     */
    fun onPickedXlsxFile(uri: Uri?, journalFormat: Boolean) {
        if (uri == null) return
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            if (bytes == null) {
                _events.emit("Не удалось прочитать файл")
                return@launch
            }
            val drafts = withContext(Dispatchers.Default) {
                if (journalFormat) {
                    StudentFileParser.parseJournalXlsx(bytes)
                } else {
                    StudentFileParser.parseTemplateXlsx(bytes)
                }
            }
            _filePreview.value = FilePreview(drafts)
        }
    }

    fun dismissFilePreview() {
        _filePreview.value = null
    }

    /** Добавление учеников из предпросмотра. Дубликаты по (фамилия, имя) пропускаются. */
    fun importDrafts(drafts: List<StudentDraft>) {
        viewModelScope.launch {
            val existingKeys = students.value.map { "${it.lastName}_${it.firstName}" }.toSet()
            val candidates = drafts
                .distinctBy { "${it.lastName}_${it.firstName}" }
                .filter { "${it.lastName}_${it.firstName}" !in existingKeys }
                .map {
                    Student(
                        id = java.util.UUID.randomUUID().toString(),
                        classId = classId,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        gender = Gender.UNSPECIFIED,
                        restriction = SeatingRestriction.NONE,
                        behaviorNote = it.behaviorNote
                    )
                }
            if (candidates.isNotEmpty()) {
                studentRepository.upsertAll(candidates)
            }
            _filePreview.value = null
            _events.emit(if (candidates.isNotEmpty()) "Добавлено учеников: ${candidates.size}" else "Новых учеников не найдено")
        }
    }
}