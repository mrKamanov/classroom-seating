package com.example.classroomseating.domain.model

/**
 * Учебный класс (например, 5 "А").
 */
data class SchoolClass(
    val id: String,
    val name: String,
    val academicYear: String,
    val classTeacherName: String?
)