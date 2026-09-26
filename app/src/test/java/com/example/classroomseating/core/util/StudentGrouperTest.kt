package com.example.classroomseating.core.util

import com.example.classroomseating.domain.model.BoardPosition
import com.example.classroomseating.domain.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentGrouperTest {

    private fun lines(rows: Int, columns: Int): List<List<DeskCoord>> =
        buildDeskGrid(BoardPosition.BOTTOM, rows, columns).lines

    private fun fullWeightMap(rows: Int, columns: Int, weight: Int = 1): Map<DeskCoord, Int> =
        lines(rows, columns).flatten().associateWith { weight }

    private fun students(count: Int): List<Student> =
        List(count) { i ->
            Student(id = "s$i", classId = "c", firstName = "Имя${i % 3}", lastName = "Фамилия${i % 3}")
        }

    @Test
    fun bands_coversEveryCoordAndBalancedByStudents() {
        val grid = lines(rows = 5, columns = 4)
        val zones = StudentGrouper.partitionTerritorial(
            grid,
            fullWeightMap(5, 4),
            groupCount = 4,
            template = StudentGrouper.Template.BANDS
        )

        assertEquals(20, zones.size)
        grid.forEach { line -> line.forEach { assertTrue(zones.containsKey(it)) } }
        val sizes = zones.values.groupingBy { it }.eachCount()
        assertEquals((0 until 4).toSet(), sizes.keys)
        assertTrue("размеры зон должны быть сбалансированы: $sizes", sizes.values.max() - sizes.values.min() <= 2)
    }

    @Test
    fun columns_cutsWholeColumnsBalanced() {
        val grid = lines(rows = 6, columns = 6)
        val zones = StudentGrouper.partitionTerritorial(
            grid,
            fullWeightMap(6, 6),
            groupCount = 3,
            template = StudentGrouper.Template.COLUMNS
        )
        val sizes = zones.values.groupingBy { it }.eachCount()
        assertEquals(3, sizes.size)
        assertEquals(12, sizes[0])
        assertEquals(12, sizes[1])
        assertEquals(12, sizes[2])
    }

    @Test
    fun blocks_fourGroupsFormTwoByTwoBlocks() {
        val grid = lines(rows = 4, columns = 4)
        val zones = StudentGrouper.partitionTerritorial(
            grid,
            fullWeightMap(4, 4),
            groupCount = 4,
            template = StudentGrouper.Template.BLOCKS
        )
        val sizes = zones.values.groupingBy { it }.eachCount()
        assertEquals(4, sizes.size)
        sizes.values.forEach { assertEquals(4, it) }
    }

    @Test
    fun blocks_dimsNearSquare() {
        assertEquals(2 to 2, StudentGrouper.blockDims(4))
        assertEquals(3 to 2, StudentGrouper.blockDims(6))
        assertEquals(5 to 1, StudentGrouper.blockDims(5))
        assertEquals(4 to 2, StudentGrouper.blockDims(8))
    }

    @Test
    fun snake_coversAllCoordsBalanced() {
        val grid = lines(rows = 5, columns = 5)
        val zones = StudentGrouper.partitionTerritorial(
            grid,
            fullWeightMap(5, 5),
            groupCount = 5,
            template = StudentGrouper.Template.SNAKE
        )
        assertEquals(25, zones.size)
        val sizes = zones.values.groupingBy { it }.eachCount()
        assertEquals(5, sizes.size)
        assertTrue("snake: $sizes", sizes.values.max() - sizes.values.min() <= 2)
    }

    @Test
    fun balancedCut_balancesByWeightEvenWithEmptySlots() {
        // 7 парт: 4 заняты (вес 5), 3 пустые (вес 0) — разрез должен дать ровно 20/4 = 5.
        val weights = intArrayOf(5, 0, 5, 0, 5, 0, 5)
        val zones = StudentGrouper.balancedCut(weights, 4)
        assertTrue("зоны возрастают: ${zones.toList()}", zones.toList().zipWithNext().all { it.second >= it.first })
        val sums = IntArray(4)
        weights.forEachIndexed { i, w -> sums[zones[i]] += w }
        assertEquals(5, sums[0])
        assertEquals(5, sums[1])
        assertEquals(5, sums[2])
        assertEquals(5, sums[3])
    }

    @Test
    fun balancedCut_firstCellIsInFirstGroup() {
        val zones = StudentGrouper.balancedCut(IntArray(20) { 1 }, 4)
        assertEquals(0, zones[0])
        assertEquals(3, zones[19])
        assertTrue(zones.toList().zipWithNext().all { it.second >= it.first })
    }

    @Test
    fun balancedCut_moreGroupsThanCells_noZonesOnEmptyTail() {
        // 6 колонок, а групп запрошено 8: разрез не должен выходить за 0..5
        // (иначе появляются пустые «хвостовые» группы — перекос по количеству).
        val zones = StudentGrouper.balancedCut(IntArray(6) { 1 }, 8)
        assertEquals(0, zones[0])
        assertTrue("максимум зоны: ${zones.maxOrNull()}", (zones.maxOrNull() ?: 0) <= 5)
        assertTrue(zones.toList().zipWithNext().all { it.second >= it.first })
    }

    @Test
    fun columns_groupCountBeyondColumns_zoneCountDoesNotExceedColumns() {
        // 6 колонок, 20 групп: зон не должно быть больше, чем колонок, иначе
        // появятся пустые группы при отрисовке кластеров.
        val grid = lines(rows = 6, columns = 5)
        val zones = StudentGrouper.partitionTerritorial(
            grid,
            fullWeightMap(6, 5),
            groupCount = 20,
            template = StudentGrouper.Template.COLUMNS
        )
        val maxZone = zones.values.maxOrNull() ?: 0
        assertTrue("зон больше допустимых колонок: $maxZone", maxZone <= 5)
        assertTrue("все зоны присутствуют", ((0..maxZone).toSet() - zones.values.toSet()).isEmpty())
    }

    @Test
    fun columns_balancesByStudentCountNotColumnCount() {
        // 36 учеников (по одному за партой) в 6 колонках на 4 группы:
        // сумма весов зон близка к 36/4, отклонение — не больше колонки (6 чел.).
        val grid = lines(rows = 6, columns = 6)
        val weights = fullWeightMap(6, 6, weight = 1)
        val zones = StudentGrouper.partitionTerritorial(
            grid,
            weights,
            groupCount = 4,
            template = StudentGrouper.Template.COLUMNS
        )
        val sums = IntArray(4)
        weights.forEach { (coord, weight) -> sums[zones.getValue(coord)] += weight }
        assertTrue("веса зон: ${sums.toList()}", sums.max() - sums.min() <= 6)
    }

    @Test
    fun chunk_balancedAcrossGroupsInOrder() {
        val grouped = StudentGrouper.chunk(students(30), count = 4)
        assertEquals(4, grouped.size)
        val sizes = grouped.map { it.size }
        assertTrue("размеры групп: $sizes", sizes.max() - sizes.min() <= 1)
        assertEquals(30, grouped.sumOf { it.size })
        assertEquals("s0", grouped.first().first().id)
        assertEquals("s29", grouped.last().last().id)
    }

    @Test
    fun chunk_preservesInputOrder() {
        val grouped = StudentGrouper.chunk(students(7), count = 2)
        val flat = grouped.flatten()
        assertEquals((0 until 7).map { "s$it" }, flat.map { it.id })
    }

    @Test
    fun chunk_emptyAndCountOverflow() {
        assertEquals(4, StudentGrouper.chunk(emptyList(), count = 4).size)
        assertEquals(3, StudentGrouper.chunk(students(3), count = 10).size)
    }

    @Test
    fun alphabetical_sortsByLastNameThenFirstName() {
        val source = listOf(
            Student(id = "3", classId = "c", firstName = "Б", lastName = "Иванов"),
            Student(id = "2", classId = "c", firstName = "А", lastName = "Иванов"),
            Student(id = "1", classId = "c", firstName = "А", lastName = "Петров"),
            Student(id = "0", classId = "c", firstName = "И", lastName = "Абрамов")
        )
        val sorted = StudentGrouper.alphabetical(source)
        assertEquals(listOf("0", "2", "3", "1"), sorted.map { it.id })
    }
}