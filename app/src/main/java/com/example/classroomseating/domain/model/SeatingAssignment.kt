package com.example.classroomseating.domain.model

/**
 * Назначение ученика на место в схеме рассадки.
 */
data class SeatingAssignment(
    val id: String,
    val seatingPlanId: String,
    val studentId: String,
    val deskId: String,
    val seatIndex: Int = 0
)