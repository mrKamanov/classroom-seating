package com.example.classroomseating.domain.repository

import com.example.classroomseating.domain.model.SchoolClass
import kotlinx.coroutines.flow.Flow

/** Класс на главном экране вместе с количеством учеников. */
data class SchoolClassSummary(
    val schoolClass: SchoolClass,
    val studentCount: Int
)

interface SchoolClassRepository {

    fun observeAll(): Flow<List<SchoolClass>>

    fun observeWithStudentCounts(): Flow<List<SchoolClassSummary>>

    fun observeById(id: String): Flow<SchoolClass?>

    suspend fun getById(id: String): SchoolClass?

    suspend fun getByName(name: String): SchoolClass?

    suspend fun upsert(schoolClass: SchoolClass)

    suspend fun delete(schoolClass: SchoolClass)
}