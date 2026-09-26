package com.example.classroomseating.core.util

import com.example.classroomseating.domain.model.BoardPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class DeskGridTest {

    // rows = «Рядов» (полосы парт вдоль доски), columns = «Парт в ряду» (глубина).

    @Test
    fun `top board keeps rows from the board downwards`() {
        val grid = buildDeskGrid(BoardPosition.TOP, rows = 3, columns = 4)

        // Парт в ряду (глубина) = 4 строки, в каждой по 3 ряда.
        assertEquals(4, grid.lines.size)
        assertEquals(3, grid.lines.first().size)

        // Сначала парта у доски (gridY = 0), затем глубже.
        assertEquals(listOf(0, 1, 2, 3), grid.lines.map { it.first().gridY })
        assertEquals(
            listOf(DeskCoord(0, 0), DeskCoord(1, 0), DeskCoord(2, 0)),
            grid.lines.first()
        )
    }

    @Test
    fun `bottom board mirrors rows so desk at board is drawn at bottom`() {
        val grid = buildDeskGrid(BoardPosition.BOTTOM, rows = 3, columns = 4)

        assertEquals(4, grid.lines.size)
        assertEquals(listOf(3, 2, 1, 0), grid.lines.map { it.first().gridY })
        // gridY = 0 (парта у доски) — последняя строка, прямо над нижней доской.
        assertEquals(0, grid.lines.last().first().gridY)
    }

    @Test
    fun `left board rows spread away from the board to the right`() {
        val grid = buildDeskGrid(BoardPosition.LEFT, rows = 3, columns = 4)

        // Слева доска: строки = ряды (3), в каждом ряду парты от доски (4).
        assertEquals(3, grid.lines.size)
        assertEquals(4, grid.lines.first().size)

        // Ряд 0 — верхняя строка, парта 1 (gridY = 0) у доски слева.
        assertEquals(0, grid.lines.first().first().gridX)
        assertEquals(
            listOf(DeskCoord(0, 0), DeskCoord(0, 1), DeskCoord(0, 2), DeskCoord(0, 3)),
            grid.lines.first()
        )
    }

    @Test
    fun `right board mirrors desks inside a row so desk at board is rightmost`() {
        val grid = buildDeskGrid(BoardPosition.RIGHT, rows = 3, columns = 4)

        assertEquals(3, grid.lines.size)
        assertEquals(4, grid.lines.first().size)

        // Парта у доски (gridY = 0) — последняя в ряду, справа.
        assertEquals(0, grid.lines.first().last().gridY)
        assertEquals(
            listOf(DeskCoord(0, 3), DeskCoord(0, 2), DeskCoord(0, 1), DeskCoord(0, 0)),
            grid.lines.first()
        )
    }

    @Test
    fun `clamps to at least one row and one desk per row`() {
        val grid = buildDeskGrid(BoardPosition.BOTTOM, rows = 0, columns = 0)

        assertEquals(1, grid.lines.size)
        assertEquals(1, grid.lines.first().size)
        assertEquals(DeskCoord(0, 0), grid.lines.first().first())
    }
}