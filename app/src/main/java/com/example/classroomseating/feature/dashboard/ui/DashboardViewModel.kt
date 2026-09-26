package com.example.classroomseating.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classroomseating.domain.model.SchoolClass
import com.example.classroomseating.domain.repository.SchoolClassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClassListItem(
    val schoolClass: SchoolClass,
    val studentsCount: Int
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val schoolClassRepository: SchoolClassRepository
) : ViewModel() {

    /** Один запрос Room с подзапросом на количество учеников — список обновляется реактивно. */
    val classes: StateFlow<List<ClassListItem>> = schoolClassRepository.observeWithStudentCounts()
        .map { summaries ->
            summaries.map { ClassListItem(it.schoolClass, it.studentCount) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun deleteClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            schoolClassRepository.delete(schoolClass)
            _messages.send("Класс «${schoolClass.name}» удалён")
        }
    }

    fun createClass(name: String, academicYear: String, classTeacherName: String?): String {
        val id = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            schoolClassRepository.upsert(
                SchoolClass(
                    id = id,
                    name = name,
                    academicYear = academicYear,
                    classTeacherName = classTeacherName
                )
            )
        }
        return id
    }

    fun updateClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            schoolClassRepository.upsert(schoolClass)
        }
    }
}