package com.example.classroomseating.core.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactQrJsonTest {

    private fun bigDto(studentCount: Int = 40): SeatingPlanExportDto {
        val desks = (0 until 12).map { i ->
            SeatingPlanExportDto.DeskDto("desk-uuid-$i", i % 4, i / 4, 2)
        }
        val students = (0 until studentCount).map { i ->
            SeatingPlanExportDto.StudentDto(
                id = "student-uuid-$i-aaaaaaaaaaaaa",
                firstName = "Имя$i",
                lastName = "Фамилия$i",
                gender = "UNSPECIFIED"
            )
        }
        val assignments = students.mapIndexed { i, s ->
            SeatingPlanExportDto.AssignmentDto(
                studentId = s.id,
                deskId = desks[i % desks.size].id,
                seatIndex = i % 2
            )
        }
        return SeatingPlanExportDto(
            schoolClass = SeatingPlanExportDto.SchoolClassDto(
                name = "5 А",
                academicYear = "2026-2027",
                classTeacherName = null
            ),
            classroom = SeatingPlanExportDto.ClassroomDto(
                name = "Кабинет 304",
                columnsCount = 4,
                rowsCount = 3,
                boardPosition = "TOP"
            ),
            desks = desks,
            students = students,
            assignments = assignments
        )
    }

    @Test
    fun compactJsonOmitsDefaultAndNullFields() {
        val raw = bigDto().toCompactQrJson()

        // exportedAt имеет динамический дефолт (Instant.now()) — его наличие не проверяем.
        assertFalse(raw.contains("appVersion"))
        assertFalse(raw.contains("visionConstraint"))
        assertFalse(raw.contains("seatingRestriction"))
        assertFalse(raw.contains("behaviorNote"))
        assertFalse(raw.contains("classTeacherName"))
        assertTrue(raw.contains("gender"))
    }

    @Test
    fun compactJsonReplacesLongUuidsWithShortKeys() {
        val raw = bigDto().toCompactQrJson()

        assertFalse(raw.contains("student-uuid"))
        assertFalse(raw.contains("desk-uuid"))
        assertTrue(raw.contains("\"s0\""))
        assertTrue(raw.contains("\"d0\""))
    }

    @Test
    fun compactJsonIsSmallerThanFullEncode() {
        val dto = bigDto()
        val full = dto.encodeToJson(
            kotlinx.serialization.json.Json {
                encodeDefaults = true
                explicitNulls = false
            }
        )
        val compact = dto.toCompactQrJson()

        assertTrue("Компактная версия должна быть меньше: ${compact.length} vs ${full.length}",
            compact.length < full.length)
    }

    @Test
    fun compactJsonStillFitsQrBudgetForLargeClass() {
        val dto = bigDto(50)
        val compressed = QrCodeCompressor.compressToQrString(dto.toCompactQrJson())

        assertTrue("Сжатый payload для 50 учеников должен помещаться в QR: ${compressed.length} байт",
            compressed.length <= QR_MAX_STORAGE_CHARS)
    }
}
