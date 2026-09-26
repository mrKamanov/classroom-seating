package com.example.classroomseating.data.mapper

import com.example.classroomseating.core.database.entity.ClassroomEntity
import com.example.classroomseating.core.database.entity.DeskEntity
import com.example.classroomseating.core.database.entity.DeskWithStudents as DeskWithStudentsEntity
import com.example.classroomseating.core.database.entity.FullSeatingPlanRelation
import com.example.classroomseating.core.database.entity.SchoolClassEntity
import com.example.classroomseating.core.database.entity.SeatingAssignmentEntity
import com.example.classroomseating.core.database.entity.SeatingPlanEntity
import com.example.classroomseating.core.database.entity.StudentEntity
import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.DeskWithStudents as DeskWithStudentsDomain
import com.example.classroomseating.domain.model.FullSeatingPlan
import com.example.classroomseating.domain.model.Gender
import com.example.classroomseating.domain.model.SchoolClass
import com.example.classroomseating.domain.model.SeatingAssignment
import com.example.classroomseating.domain.model.SeatingPlan
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student

fun SchoolClassEntity.toDomain(): SchoolClass = SchoolClass(
    id = id,
    name = name,
    academicYear = academicYear,
    classTeacherName = classTeacherName
)

fun SchoolClass.toEntity(): SchoolClassEntity = SchoolClassEntity(
    id = id,
    name = name,
    academicYear = academicYear,
    classTeacherName = classTeacherName
)

fun StudentEntity.toDomain(): Student = Student(
    id = id,
    classId = classId,
    firstName = firstName,
    lastName = lastName,
    gender = gender.toDomain(),
    restriction = SeatingRestriction.fromLegacy(visionConstraint, restriction),
    behaviorNote = behaviorNote,
    tagColorHex = tagColorHex
)

fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    classId = classId,
    firstName = firstName,
    lastName = lastName,
    gender = gender.toEntity(),
    restriction = restriction.name,
    visionConstraint = restriction == SeatingRestriction.VISION,
    behaviorNote = behaviorNote,
    tagColorHex = tagColorHex
)

fun com.example.classroomseating.core.database.entity.Gender.toDomain(): Gender = when (this) {
    com.example.classroomseating.core.database.entity.Gender.MALE -> Gender.MALE
    com.example.classroomseating.core.database.entity.Gender.FEMALE -> Gender.FEMALE
    com.example.classroomseating.core.database.entity.Gender.UNSPECIFIED -> Gender.UNSPECIFIED
}

fun Gender.toEntity(): com.example.classroomseating.core.database.entity.Gender = when (this) {
    Gender.MALE -> com.example.classroomseating.core.database.entity.Gender.MALE
    Gender.FEMALE -> com.example.classroomseating.core.database.entity.Gender.FEMALE
    Gender.UNSPECIFIED -> com.example.classroomseating.core.database.entity.Gender.UNSPECIFIED
}

fun ClassroomEntity.toDomain(): Classroom = Classroom(
    id = id,
    name = name,
    columnsCount = columnsCount,
    rowsCount = rowsCount,
    boardPosition = boardPosition.toDomain()
)

fun Classroom.toEntity(): ClassroomEntity = ClassroomEntity(
    id = id,
    name = name,
    columnsCount = columnsCount,
    rowsCount = rowsCount,
    boardPosition = boardPosition.toEntity()
)

fun com.example.classroomseating.core.database.entity.BoardPosition.toDomain(): com.example.classroomseating.domain.model.BoardPosition =
    when (this) {
        com.example.classroomseating.core.database.entity.BoardPosition.TOP ->
            com.example.classroomseating.domain.model.BoardPosition.TOP
        com.example.classroomseating.core.database.entity.BoardPosition.BOTTOM ->
            com.example.classroomseating.domain.model.BoardPosition.BOTTOM
        com.example.classroomseating.core.database.entity.BoardPosition.LEFT ->
            com.example.classroomseating.domain.model.BoardPosition.LEFT
        com.example.classroomseating.core.database.entity.BoardPosition.RIGHT ->
            com.example.classroomseating.domain.model.BoardPosition.RIGHT
    }

fun com.example.classroomseating.domain.model.BoardPosition.toEntity(): com.example.classroomseating.core.database.entity.BoardPosition =
    when (this) {
        com.example.classroomseating.domain.model.BoardPosition.TOP ->
            com.example.classroomseating.core.database.entity.BoardPosition.TOP
        com.example.classroomseating.domain.model.BoardPosition.BOTTOM ->
            com.example.classroomseating.core.database.entity.BoardPosition.BOTTOM
        com.example.classroomseating.domain.model.BoardPosition.LEFT ->
            com.example.classroomseating.core.database.entity.BoardPosition.LEFT
        com.example.classroomseating.domain.model.BoardPosition.RIGHT ->
            com.example.classroomseating.core.database.entity.BoardPosition.RIGHT
    }

fun DeskEntity.toDomain(): Desk = Desk(
    id = id,
    classroomId = classroomId,
    gridX = gridX,
    gridY = gridY,
    capacity = capacity,
    label = label
)

fun Desk.toEntity(): DeskEntity = DeskEntity(
    id = id,
    classroomId = classroomId,
    gridX = gridX,
    gridY = gridY,
    capacity = capacity,
    label = label
)

fun SeatingPlanEntity.toDomain(): SeatingPlan = SeatingPlan(
    id = id,
    classId = classId,
    classroomId = classroomId,
    title = title,
    isDefault = isDefault,
    updatedAt = updatedAt
)

fun SeatingPlan.toEntity(): SeatingPlanEntity = SeatingPlanEntity(
    id = id,
    classId = classId,
    classroomId = classroomId,
    title = title,
    isDefault = isDefault,
    updatedAt = updatedAt
)

fun SeatingAssignmentEntity.toDomain(): SeatingAssignment = SeatingAssignment(
    id = id,
    seatingPlanId = seatingPlanId,
    studentId = studentId,
    deskId = deskId,
    seatIndex = seatIndex
)

fun DeskWithStudentsEntity.toDomain(): DeskWithStudentsDomain = DeskWithStudentsDomain(
    desk = desk.toDomain(),
    students = students.map { it.toDomain() }
)

fun FullSeatingPlanRelation.toDomain(): FullSeatingPlan = FullSeatingPlan(
    plan = seatingPlan.toDomain(),
    desksWithStudents = desksWithStudents.map { it.toDomain() }
)