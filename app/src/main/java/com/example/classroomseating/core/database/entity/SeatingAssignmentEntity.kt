package com.example.classroomseating.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Назначение ученика на конкретное место в схеме рассадки.
 */
@Entity(
    tableName = "seating_assignments",
    foreignKeys = [
        ForeignKey(
            entity = SeatingPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["seatingPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DeskEntity::class,
            parentColumns = ["id"],
            childColumns = ["deskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["seatingPlanId"]),
        Index(value = ["studentId"]),
        Index(value = ["deskId"]),
        Index(value = ["seatingPlanId", "studentId"], unique = true)
    ]
)
data class SeatingAssignmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val seatingPlanId: String,
    val studentId: String,
    val deskId: String,
    val seatIndex: Int = 0
)