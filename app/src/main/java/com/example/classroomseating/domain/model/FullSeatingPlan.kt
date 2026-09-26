package com.example.classroomseating.domain.model

/**
 * Парта с посаженными учениками.
 */
data class DeskWithStudents(
    val desk: Desk,
    val students: List<Student>
)

/**
 * Полная схема рассадки для экрана.
 */
data class FullSeatingPlan(
    val plan: SeatingPlan,
    val desksWithStudents: List<DeskWithStudents>
)