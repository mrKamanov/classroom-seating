package com.example.classroomseating.domain.model

enum class Gender { MALE, FEMALE, UNSPECIFIED }

/**
 * Ученик с метками/ограничениями по здоровью и поведению.
 */
data class Student(
    val id: String,
    val classId: String,
    val firstName: String,
    val lastName: String,
    val gender: Gender = Gender.UNSPECIFIED,
    val restriction: SeatingRestriction = SeatingRestriction.NONE,
    val behaviorNote: String? = null,
    val tagColorHex: String? = null
)