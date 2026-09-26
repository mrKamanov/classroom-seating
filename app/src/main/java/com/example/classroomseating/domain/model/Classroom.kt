package com.example.classroomseating.domain.model

enum class BoardPosition { TOP, BOTTOM, LEFT, RIGHT }

/**
 * Физический кабинет и его сетка парт.
 */
data class Classroom(
    val id: String,
    val name: String,
    val columnsCount: Int = 6,
    val rowsCount: Int = 5,
    val boardPosition: BoardPosition = BoardPosition.TOP
)