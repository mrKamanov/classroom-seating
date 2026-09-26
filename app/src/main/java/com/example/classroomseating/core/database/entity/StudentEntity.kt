package com.example.classroomseating.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class Gender { MALE, FEMALE, UNSPECIFIED }

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class StudentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    val firstName: String,
    val lastName: String,
    val gender: Gender = Gender.UNSPECIFIED,
    val restriction: String = "NONE",
    val visionConstraint: Boolean = false,
    val behaviorNote: String? = null,
    val tagColorHex: String? = null
)