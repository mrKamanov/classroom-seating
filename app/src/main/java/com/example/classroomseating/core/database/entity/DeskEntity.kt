package com.example.classroomseating.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Парта в конкретном кабинете.
 */
@Entity(
    tableName = "desks",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classroomId"])]
)
data class DeskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val classroomId: String,
    val gridX: Int,
    val gridY: Int,
    val capacity: Int = 2,
    val label: String? = null
)