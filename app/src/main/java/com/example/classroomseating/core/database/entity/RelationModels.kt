package com.example.classroomseating.core.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Парта со списком посаженных на неё учеников в рамках схемы рассадки.
 */
data class DeskWithStudents(
    @Embedded val desk: DeskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SeatingAssignmentEntity::class,
            parentColumn = "deskId",
            entityColumn = "studentId"
        )
    )
    val students: List<StudentEntity>
)

/**
 * Полная схема рассадки: план + все парты кабинета с учениками.
 */
data class FullSeatingPlanRelation(
    @Embedded val seatingPlan: SeatingPlanEntity,
    @Relation(
        parentColumn = "classroomId",
        entityColumn = "classroomId",
        entity = DeskEntity::class
    )
    val desksWithStudents: List<DeskWithStudents>
)