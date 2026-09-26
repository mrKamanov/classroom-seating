package com.example.classroomseating.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StudentListParserTest {

    @Test
    fun parsesSimpleLines() {
        val parsed = StudentListParser.parse("Иванов Иван\nПетров Пётр")
        assertEquals(2, parsed.size)
        assertEquals(StudentDraft("Иванов", "Иван"), parsed[0])
        assertEquals(StudentDraft("Петров", "Пётр"), parsed[1])
    }

    @Test
    fun parsesNumberedAndBulletedLines() {
        val parsed = StudentListParser.parse("1. Смирнов Алексей\n-) Ковалёва Елена\n- Сидоров Олег")
        assertEquals(3, parsed.size)
        assertEquals(StudentDraft("Смирнов", "Алексей"), parsed[0])
        assertEquals(StudentDraft("Ковалёва", "Елена"), parsed[1])
        assertEquals(StudentDraft("Сидоров", "Олег"), parsed[2])
    }

    @Test
    fun parsesTabSeparated() {
        val parsed = StudentListParser.parse("Иванов\tИван\nПетров\tПётр")
        assertEquals(StudentDraft("Иванов", "Иван"), parsed[0])
        assertEquals(StudentDraft("Петров", "Пётр"), parsed[1])
    }

    @Test
    fun ignoresEmptyAndBlankLines() {
        val parsed = StudentListParser.parse("\nИванов Иван\n\n   \nПетров Пётр\n")
        assertEquals(2, parsed.size)
    }

    @Test
    fun ignoresPrefixNumberWithoutNameSuffix() {
        assertEquals(1, StudentListParser.parse("1. Иванов Иван").size)
    }

    @Test
    fun handlesSingleWordLineAsLastName() {
        val parsed = StudentListParser.parse("Иванов")
        assertEquals(StudentDraft("Иванов", ""), parsed[0])
    }

    @Test
    fun handlesExtraMiddleNamesAsPartOfFirstNameTokens() {
        val parsed = StudentListParser.parse("Иванов Иван Петрович")
        assertEquals(StudentDraft("Иванов", "Иван"), parsed[0])
    }

    @Test
    fun capitalizesFirstLettersOfNames() {
        val parsed = StudentListParser.parse("иванов иван\nпетрова анна")
        assertEquals(StudentDraft("Иванов", "Иван"), parsed[0])
        assertEquals(StudentDraft("Петрова", "Анна"), parsed[1])
    }
}