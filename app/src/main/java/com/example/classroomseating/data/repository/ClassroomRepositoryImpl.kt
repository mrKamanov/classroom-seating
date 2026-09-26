package com.example.classroomseating.data.repository

import com.example.classroomseating.core.database.dao.ClassroomDao
import com.example.classroomseating.data.mapper.toDomain
import com.example.classroomseating.data.mapper.toEntity
import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.repository.ClassroomRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ClassroomRepositoryImpl @Inject constructor(
    private val dao: ClassroomDao
) : ClassroomRepository {

    override fun observeAll(): Flow<List<Classroom>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Classroom?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Classroom? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(classroom: Classroom): String {
        dao.insert(classroom.toEntity())
        return classroom.id
    }

    override suspend fun delete(classroom: Classroom) {
        dao.delete(classroom.toEntity())
    }

    override fun observeDesks(classroomId: String): Flow<List<Desk>> =
        dao.observeDesks(classroomId).map { list -> list.map { it.toDomain() } }

    override suspend fun replaceDesks(classroomId: String, desks: List<Desk>) {
        dao.deleteDesks(classroomId)
        dao.insertDesks(desks.map { it.toEntity() })
    }

    override suspend fun deleteDesk(deskId: String) {
        dao.deleteDesk(deskId)
    }
}