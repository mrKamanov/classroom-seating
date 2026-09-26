package com.example.classroomseating.domain.model

/**
 * Парта: физическое место в кабинете.
 */
data class Desk(
    val id: String,
    val classroomId: String,
    val gridX: Int,
    val gridY: Int,
    val capacity: Int = 2,
    val label: String? = null
)