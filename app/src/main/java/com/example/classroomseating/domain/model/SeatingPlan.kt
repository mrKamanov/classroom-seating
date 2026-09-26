package com.example.classroomseating.domain.model

/**
 * Схема рассадки класса в конкретном кабинете.
 */
data class SeatingPlan(
    val id: String,
    val classId: String,
    val classroomId: String,
    val title: String,
    val isDefault: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)