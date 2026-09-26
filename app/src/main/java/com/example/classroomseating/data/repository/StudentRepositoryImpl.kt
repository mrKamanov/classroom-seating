package com.example.classroomseating.data.repository

import com.example.classroomseating.core.database.dao.StudentDao
import com.example.classroomseating.data.mapper.toDomain
import com.example.classroomseating.data.mapper.toEntity
import com.example.classroomseating.domain.model.Student
import com.example.classroomseating.domain.repository.StudentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val dao: StudentDao
) : StudentRepository {

    override fun observeByClass(classId: String): Flow<List<Student>> =
        dao.observeByClass(classId).map { list -> list.map { it.toDomain() } }

    override fun observeCount(classId: String): Flow<Int> =
        dao.observeCount(classId)

    override suspend fun getByIds(ids: List<String>): List<Student> =
        dao.getByIds(ids).map { it.toDomain() }

    override suspend fun upsert(student: Student) {
        dao.insert(student.toEntity())
    }

    override suspend fun upsertAll(students: List<Student>) {
        dao.insertAll(students.map { it.toEntity() })
    }

    override suspend fun delete(studentId: String) {
        dao.deleteById(studentId)
    }

    override suspend fun moveToClass(studentId: String, newClassId: String) {
        dao.moveToClass(studentId, newClassId)
    }
}