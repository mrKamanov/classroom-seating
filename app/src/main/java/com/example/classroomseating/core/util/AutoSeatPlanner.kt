package com.example.classroomseating.core.util

import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.RestrictionRule
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student
import kotlin.math.abs
import kotlin.random.Random

/**
 * Планировщик автоматической рассадки (3.3).
 *
 * Каждый ученик садится на наиболее подходящее свободное место:
 * 1. сначала место, попадающее в зону ограничения по здоровью;
 * 2. затем — ближе к «целевой» глубине (у доски или дальше от неё);
 * 3. при необходимости центра — ближе к середине класса.
 *
 * Ученики с самыми строгими зонами (первая парта + центр) обрабатываются
 * раньше остальных, чтобы не потерять подходящее место. Внутри равных
 * условий сохраняется алфавитный порядок (фамилия, затем имя).
 *
 * При передаче [random] границы категорий сохраняются, но внутри каждого
 * приоритета ученики перемешиваются, а из равно-подходящих мест и «ничьих»
 * выбирается случайное — как бросок костей для поиска лучшего варианта.
 *
 * Координаты: gridX — номер ряда (вдоль доски), gridY — номер парты от доски.
 */
object AutoSeatPlanner {

    data class SeatPlanEntry(
        val studentId: String,
        val deskId: String,
        val seatIndex: Int
    )

    private data class SeatRef(
        val deskId: String,
        val seatIndex: Int,
        val gridX: Int,
        val gridY: Int
    )

    fun arrange(
        students: List<Student>,
        desks: List<Desk>,
        random: Random? = null
    ): List<SeatPlanEntry> {
        if (students.isEmpty()) return emptyList()

        val seats = desks.flatMap { desk ->
            (0 until desk.capacity.coerceAtLeast(1)).map { index ->
                SeatRef(desk.id, index, desk.gridX, desk.gridY)
            }
        }
        if (seats.isEmpty()) return emptyList()

        val rows = (desks.maxOfOrNull { it.gridX } ?: 0) + 1
        val columns = (desks.maxOfOrNull { it.gridY } ?: 0) + 1
        val centerX = (rows - 1) / 2

        val remaining = seats.toMutableList()
        val ordered = orderStudents(students, random)

        return ordered.mapNotNull { student ->
            if (remaining.isEmpty()) return@mapNotNull null
            val best = pickSeat(remaining, student.restriction, rows, columns, centerX, random)
                ?: return@mapNotNull null
            remaining.remove(best)
            SeatPlanEntry(student.id, best.deskId, best.seatIndex)
        }
    }

    /** Приоритеты строгие и неизменны; случайность — только внутри одной группы. */
    private fun orderStudents(students: List<Student>, random: Random?): List<Student> =
        students
            .sortedWith(
                compareBy<Student> { priority(it.restriction) }
                    .thenBy { it.lastName.lowercase() }
                    .thenBy { it.firstName.lowercase() }
            )
            .groupBy { priority(it.restriction) }
            .toSortedMap()
            .values
            .flatMap { tier ->
                if (random == null) tier else tier.shuffled(random)
            }

    private fun pickSeat(
        remaining: MutableList<SeatRef>,
        restriction: SeatingRestriction,
        rows: Int,
        columns: Int,
        centerX: Int,
        random: Random?
    ): SeatRef? {
        if (random == null) {
            return remaining.minWithOrNull(seatComparator(restriction, rows, columns, centerX))
        }
        var bestKey: List<Int>? = null
        val ties = ArrayList<SeatRef>()
        for (seat in remaining) {
            val key = seatKeys(seat, restriction, rows, columns, centerX)
            when {
                bestKey == null -> {
                    bestKey = key
                    ties.add(seat)
                }
                compareIntLists(key, bestKey) < 0 -> {
                    bestKey = key
                    ties.clear()
                    ties.add(seat)
                }
                compareIntLists(key, bestKey) == 0 -> ties.add(seat)
            }
        }
        return if (ties.isEmpty()) null else ties.random(random)
    }

    private fun compareIntLists(a: List<Int>, b: List<Int>): Int {
        for (i in a.indices) {
            val cmp = a[i].compareTo(b[i])
            if (cmp != 0) return cmp
        }
        return 0
    }

    private fun priority(restriction: SeatingRestriction): Int = when (restriction.rule) {
        RestrictionRule.FRONT_CENTER -> 0
        RestrictionRule.MIDDLE_CENTER -> 1
        RestrictionRule.FRONT -> 2
        RestrictionRule.BACK -> 3
        RestrictionRule.CENTER -> 4
        RestrictionRule.ANY -> 5
    }

    private fun seatComparator(
        restriction: SeatingRestriction,
        rows: Int,
        columns: Int,
        centerX: Int
    ): Comparator<SeatRef> = Comparator { a, b ->
        val keysA = seatKeys(a, restriction, rows, columns, centerX)
        val keysB = seatKeys(b, restriction, rows, columns, centerX)
        var result = 0
        for (index in keysA.indices) {
            result = keysA[index].compareTo(keysB[index])
            if (result != 0) break
        }
        if (result != 0) {
            result
        } else {
            val byDesk = a.deskId.compareTo(b.deskId)
            if (byDesk != 0) byDesk else a.seatIndex.compareTo(b.seatIndex)
        }
    }

    private fun seatKeys(
        seat: SeatRef,
        restriction: SeatingRestriction,
        rows: Int,
        columns: Int,
        centerX: Int
    ): List<Int> {
        val rule = restriction.rule
        val suitability = if (rule.isSuitableAt(seat.gridX, seat.gridY, rows, columns)) 0 else 1
        val depth = abs(seat.gridY - targetDepth(rule, columns))
        val lateral = if (prefersCenter(rule)) abs(seat.gridX - centerX) else seat.gridX
        return listOf(suitability, depth, lateral)
    }

    private fun targetDepth(rule: RestrictionRule, columns: Int): Int = when (rule) {
        RestrictionRule.BACK -> columns - 1
        RestrictionRule.MIDDLE_CENTER -> 1
        else -> 0
    }

    private fun prefersCenter(rule: RestrictionRule): Boolean =
        rule == RestrictionRule.FRONT_CENTER ||
            rule == RestrictionRule.MIDDLE_CENTER ||
            rule == RestrictionRule.CENTER
}
