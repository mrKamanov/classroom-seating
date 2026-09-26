package com.example.classroomseating.data.repository

import com.example.classroomseating.core.database.dao.SchoolClassDao
import com.example.classroomseating.data.mapper.toDomain
import com.example.classroomseating.data.mapper.toEntity
import com.example.classroomseating.domain.model.SchoolClass
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SchoolClassSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SchoolClassRepositoryImpl @Inject constructor(
    private val dao: SchoolClassDao
) : SchoolClassRepository {

    override fun observeAll(): Flow<List<SchoolClass>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeWithStudentCounts(): Flow<List<SchoolClassSummary>> =
        dao.observeWithStudentCounts().map { rows ->
            rows.map { row ->
                SchoolClassSummary(
                    schoolClass = SchoolClass(
                        id = row.id,
                        name = row.name,
                        academicYear = row.academicYear,
                        classTeacherName = row.classTeacherName
                    ),
                    studentCount = row.studentCount
                )
            }
        }

    override fun observeById(id: String): Flow<SchoolClass?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): SchoolClass? =
        dao.getById(id)?.toDomain()

    override suspend fun getByName(name: String): SchoolClass? =
        dao.getByName(name)?.toDomain()

    override suspend fun upsert(schoolClass: SchoolClass) {
        dao.insert(schoolClass.toEntity())
    }

    override suspend fun delete(schoolClass: SchoolClass) {
        dao.deleteClassWithData(schoolClass.id)
    }
}