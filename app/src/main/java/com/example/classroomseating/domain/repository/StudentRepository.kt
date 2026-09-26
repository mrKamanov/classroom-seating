package com.example.classroomseating.domain.repository

import com.example.classroomseating.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {

    fun observeByClass(classId: String): Flow<List<Student>>

    fun observeCount(classId: String): Flow<Int>

    suspend fun getByIds(ids: List<String>): List<Student>

    suspend fun upsert(student: Student)

    suspend fun upsertAll(students: List<Student>)

    suspend fun delete(studentId: String)

    suspend fun moveToClass(studentId: String, newClassId: String)
}