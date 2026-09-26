package com.example.classroomseating.core.export

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Обменный формат `.seating` (спецификация 04_DATA_EXPORT_IMPORT.md, v1.0).
 */
@Serializable
data class SeatingPlanExportDto(
    val version: Int = 1,
    val exportedAt: String = Instant.now().toString(),
    val appVersion: String = "0.1.0",
    val schoolClass: SchoolClassDto,
    val classroom: ClassroomDto,
    val desks: List<DeskDto>,
    val students: List<StudentDto>,
    val assignments: List<AssignmentDto>
) {

    @Serializable
    data class SchoolClassDto(
        val name: String,
        val academicYear: String,
        val classTeacherName: String? = null
    )

    @Serializable
    data class ClassroomDto(
        val name: String,
        val columnsCount: Int,
        val rowsCount: Int,
        val boardPosition: String
    )

    @Serializable
    data class DeskDto(
        val id: String,
        val gridX: Int,
        val gridY: Int,
        val capacity: Int
    )

    @Serializable
    data class StudentDto(
        val id: String,
        val firstName: String,
        val lastName: String,
        val gender: String,
        val visionConstraint: Boolean = false,
        val seatingRestriction: String? = null,
        val behaviorNote: String? = null
    )

    @Serializable
    data class AssignmentDto(
        val studentId: String,
        val deskId: String,
        val seatIndex: Int
    )
}

fun SeatingPlanExportDto.encodeToJson(json: Json): String = json.encodeToString(this)

/** Безопасный бюджет QR: version 25 (EC-L) даёт ~1850 байт — код компактнее и лучше считывается с экрана. */
internal const val QR_MAX_STORAGE_CHARS: Int = 1800

private val CompactQrJson = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

/**
 * Компактная копия для QR: длинные UUID заменяются короткими ключами,
 * default/null-поля выкидываются из JSON. Импорт при этом не ломается —
 * [SeatingPlanImportService] назначает новые UUID по этим id.
 */
internal fun SeatingPlanExportDto.toCompactQrJson(): String {
    val deskKeys = desks.mapIndexed { i, d -> d.id to "d$i" }.toMap()
    val studentKeys = students.mapIndexed { i, s -> s.id to "s$i" }.toMap()
    val compact = copy(
        desks = desks.map { it.copy(id = deskKeys.getValue(it.id)) },
        students = students.map {
            it.copy(id = studentKeys.getValue(it.id))
        },
        assignments = assignments.map { a ->
            a.copy(
                studentId = studentKeys[a.studentId] ?: a.studentId,
                deskId = deskKeys[a.deskId] ?: a.deskId
            )
        }
    )
    return CompactQrJson.encodeToString(compact)
}