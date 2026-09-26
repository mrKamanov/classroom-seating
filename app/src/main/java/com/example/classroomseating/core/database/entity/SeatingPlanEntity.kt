package com.example.classroomseating.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Схема рассадки: привязка учебного класса к кабинету на учебный период.
 */
@Entity(
    tableName = "seating_plans",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classId"]),
        Index(value = ["classroomId"])
    ]
)
data class SeatingPlanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    val classroomId: String,
    val title: String,
    val isDefault: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)