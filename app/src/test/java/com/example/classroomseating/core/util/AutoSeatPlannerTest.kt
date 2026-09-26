package com.example.classroomseating.core.util

import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AutoSeatPlannerTest {

    private fun student(
        id: String,
        lastName: String,
        firstName: String = "Имя",
        restriction: SeatingRestriction = SeatingRestriction.NONE
    ): Student = Student(
        id = id,
        classId = "class",
        firstName = firstName,
        lastName = lastName,
        restriction = restriction
    )

    private fun desk(gridX: Int, gridY: Int, capacity: Int = 1): Desk = Desk(
        id = "d_${gridX}_$gridY",
        classroomId = "classroom",
        gridX = gridX,
        gridY = gridY,
        capacity = capacity
    )

    private fun grid(rows: Int, columns: Int, capacity: Int = 1): List<Desk> =
        (0 until rows).flatMap { x -> (0 until columns).map { y -> desk(x, y, capacity) } }

    @Test
    fun restrictionZonesAreRespected() {
        val desks = grid(rows = 3, columns = 4)
        val students = listOf(
            student("vision", "Абрамов", restriction = SeatingRestriction.VISION),
            student("hearing", "Борисов", restriction = SeatingRestriction.HEARING),
            student("hyper", "Волков", restriction = SeatingRestriction.HYPEROPIA),
            student("none", "Громов")
        )

        val plan = AutoSeatPlanner.arrange(students, desks)
        fun seatOf(id: String): Desk =
            plan.first { it.studentId == id }.let { entry -> desks.first { it.id == entry.deskId } }

        assertTrue("Зрение — у доски", seatOf("vision").gridY < 2)
        val hearing = seatOf("hearing")
        assertTrue("Слух — у доски", hearing.gridY < 2)
        assertEquals("Слух — центральный ряд", 1, hearing.gridX)
        assertTrue("Дальнозоркость — дальше от доски", seatOf("hyper").gridY >= 2)
    }

    @Test
    fun unrestrictedStudentsFollowAlphabetAndFillFrontFirst() {
        val desks = grid(rows = 1, columns = 4)
        val students = listOf(
            student("1", "Петров", "Пётр"),
            student("2", "Антонов", "Иван"),
            student("3", "Иванов", "Андрей"),
            student("4", "Борисов", "Борис")
        )

        val plan = AutoSeatPlanner.arrange(students, desks)
        val depthById = plan.associate { entry ->
            entry.studentId to desks.first { it.id == entry.deskId }.gridY
        }

        assertEquals(0, depthById["2"])
        assertEquals(1, depthById["4"])
        assertEquals(2, depthById["3"])
        assertEquals(3, depthById["1"])
    }

    @Test
    fun everyStudentGetsUniqueSeat() {
        val desks = grid(rows = 2, columns = 3)
        val students = (1..6).map { student("s$it", "Ученик$it") }

        val plan = AutoSeatPlanner.arrange(students, desks)

        assertEquals(6, plan.size)
        assertEquals(6, plan.map { it.deskId to it.seatIndex }.distinct().size)
        assertEquals(6, plan.map { it.studentId }.distinct().size)
    }

    @Test
    fun moreStudentsThanSeatsDropsExtra() {
        val desks = grid(rows = 1, columns = 2)
        val students = (1..3).map { student("s$it", "Ученик$it") }

        val plan = AutoSeatPlanner.arrange(students, desks)

        assertEquals(2, plan.size)
    }

    @Test
    fun emptyInputsProduceEmptyPlan() {
        assertTrue(AutoSeatPlanner.arrange(emptyList(), grid(1, 1)).isEmpty())
        assertTrue(AutoSeatPlanner.arrange(listOf(student("1", "Тестов")), emptyList()).isEmpty())
    }

    @Test
    fun shuffleKeepsRestrictionZones() {
        val desks = grid(rows = 3, columns = 4)
        val students = listOf(
            student("vision", "Абрамов", restriction = SeatingRestriction.VISION),
            student("hearing", "Борисов", restriction = SeatingRestriction.HEARING),
            student("hyper", "Волков", restriction = SeatingRestriction.HYPEROPIA),
            student("none1", "Громов"),
            student("none2", "Дроздов"),
            student("none3", "Ежов"),
            student("none4", "Жуков")
        )

        repeat(50) { seed ->
            val plan = AutoSeatPlanner.arrange(students, desks, Random(seed))
            fun seatOf(id: String): Desk =
                plan.first { it.studentId == id }.let { entry -> desks.first { it.id == entry.deskId } }

            assertTrue("Зрение — у доски", seatOf("vision").gridY < 2)
            val hearing = seatOf("hearing")
            assertTrue("Слух — у доски", hearing.gridY < 2)
            assertEquals("Слух — центральный ряд", 1, hearing.gridX)
            assertTrue("Дальнозоркость — дальше от доски", seatOf("hyper").gridY >= 2)
            assertEquals("Все рассажены", 7, plan.size)
            assertEquals("Места уникальны", 7, plan.map { it.deskId to it.seatIndex }.distinct().size)
        }
    }

    @Test
    fun shuffleProducesMultipleVariants() {
        val desks = grid(rows = 2, columns = 5)
        val students = (1..9).map { student("s$it", "Ученик$it") }

        val variants = (0 until 20).map { seed ->
            AutoSeatPlanner.arrange(students, desks, Random(seed))
                .map { it.studentId to (it.deskId to it.seatIndex) }
                .toMap()
        }.distinct().size

        assertTrue("Ожидалось несколько разных вариантов, получено $variants", variants > 1)
    }

    @Test
    fun deterministicModeIsStableAcrossCalls() {
        val desks = grid(rows = 2, columns = 4)
        val students = listOf(
            student("1", "Сидоров"),
            student("2", "Антонов"),
            student("3", "Иванов"),
            student("4", "Борисов")
        )

        val first = AutoSeatPlanner.arrange(students, desks).map { it.studentId to (it.deskId to it.seatIndex) }
        val second = AutoSeatPlanner.arrange(students, desks).map { it.studentId to (it.deskId to it.seatIndex) }

        assertEquals(first, second)
    }

    @Test
    fun sameSeedYieldsSameShuffle() {
        val desks = grid(rows = 2, columns = 5)
        val students = (1..8).map { student("s$it", "Ученик$it") }

        fun plan(seed: Long) = AutoSeatPlanner.arrange(students, desks, Random(seed))
            .map { it.studentId to (it.deskId to it.seatIndex) }

        assertEquals(plan(42L), plan(42L))
        assertNotEquals(plan(42L), plan(43L))
    }
}
