package com.example.classroomseating.data.imports

import com.example.classroomseating.core.export.SeatingPlanExportDto
import com.example.classroomseating.domain.model.BoardPosition
import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.Gender
import com.example.classroomseating.domain.model.SchoolClass
import com.example.classroomseating.domain.model.SeatingPlan
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student
import com.example.classroomseating.domain.repository.ClassroomRepository
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import com.example.classroomseating.domain.repository.StudentRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ImportMode { CREATE_COPY, REPLACE }

data class ImportResult(
    val classId: String,
    val className: String
)

/**
 * Импорт рассадки из DTO `.seating` в локальную БД (4.1, 4.3).
 */
@Singleton
class SeatingPlanImportService @Inject constructor(
    private val schoolClassRepository: SchoolClassRepository,
    private val classroomRepository: ClassroomRepository,
    private val studentRepository: StudentRepository,
    private val seatingPlanRepository: SeatingPlanRepository
) {

    suspend fun existsClassWithName(name: String): Boolean =
        schoolClassRepository.getByName(name) != null

    suspend fun import(dto: SeatingPlanExportDto, mode: ImportMode): ImportResult {
        val existing = schoolClassRepository.getByName(dto.schoolClass.name)
        var className = dto.schoolClass.name

        if (existing != null) {
            when (mode) {
                ImportMode.REPLACE -> schoolClassRepository.delete(existing)
                ImportMode.CREATE_COPY -> className = "${dto.schoolClass.name} (Импортировано)"
            }
        }

        // Класс
        val classId = UUID.randomUUID().toString()
        schoolClassRepository.upsert(
            SchoolClass(
                id = classId,
                name = className,
                academicYear = dto.schoolClass.academicYear,
                classTeacherName = dto.schoolClass.classTeacherName
            )
        )

        // Кабинет
        val classroomId = UUID.randomUUID().toString()
        classroomRepository.upsert(
            Classroom(
                id = classroomId,
                name = dto.classroom.name.ifBlank { "Кабинет" },
                columnsCount = dto.classroom.columnsCount,
                rowsCount = dto.classroom.rowsCount,
                boardPosition = parseBoardPosition(dto.classroom.boardPosition)
            )
        )

        // Парты
        val deskIdToNew = HashMap<String, String>()
        val desks = dto.desks.map { deskDto ->
            val newId = UUID.randomUUID().toString()
            deskIdToNew[deskDto.id] = newId
            Desk(
                id = newId,
                classroomId = classroomId,
                gridX = deskDto.gridX,
                gridY = deskDto.gridY,
                capacity = deskDto.capacity.coerceAtLeast(1)
            )
        }
        classroomRepository.replaceDesks(classroomId, desks)

        // Ученики
        val studentIdToNew = HashMap<String, String>()
        val students = dto.students.map { studentDto ->
            val newId = UUID.randomUUID().toString()
            studentIdToNew[studentDto.id] = newId
            Student(
                id = newId,
                classId = classId,
                firstName = studentDto.firstName,
                lastName = studentDto.lastName,
                gender = parseGender(studentDto.gender),
                restriction = SeatingRestriction.fromLegacy(
                    studentDto.visionConstraint,
                    studentDto.seatingRestriction
                ),
                behaviorNote = studentDto.behaviorNote
            )
        }
        studentRepository.upsertAll(students)

        // Схема рассадки + назначения
        val planId = seatingPlanRepository.upsertPlan(
            SeatingPlan(
                id = UUID.randomUUID().toString(),
                classId = classId,
                classroomId = classroomId,
                title = "$className — рассадка"
            )
        )
        dto.assignments.forEach { assignment ->
            val newStudentId = studentIdToNew[assignment.studentId]
            val newDeskId = deskIdToNew[assignment.deskId]
            if (newStudentId != null && newDeskId != null) {
                seatingPlanRepository.assignStudent(
                    planId = planId,
                    studentId = newStudentId,
                    deskId = newDeskId,
                    seatIndex = assignment.seatIndex
                )
            }
        }

        return ImportResult(classId = classId, className = className)
    }

    private fun parseBoardPosition(value: String): BoardPosition =
        runCatching { BoardPosition.valueOf(value) }.getOrDefault(BoardPosition.TOP)

    private fun parseGender(value: String): Gender =
        runCatching { Gender.valueOf(value) }.getOrDefault(Gender.UNSPECIFIED)
}