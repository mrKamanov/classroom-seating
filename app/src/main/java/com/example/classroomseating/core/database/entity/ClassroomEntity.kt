package com.example.classroomseating.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class BoardPosition { TOP, BOTTOM, LEFT, RIGHT }

/**
 * Физический кабинет: конфигурация сетки парт и расположение доски.
 */
@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val columnsCount: Int = 6,
    val rowsCount: Int = 5,
    val boardPosition: BoardPosition = BoardPosition.TOP
)