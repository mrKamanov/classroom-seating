package com.example.classroomseating.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Учебный класс (например, 5 "А").
 */
@Entity(tableName = "school_classes")
data class SchoolClassEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val academicYear: String,
    val classTeacherName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)