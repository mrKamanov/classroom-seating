package com.example.classroomseating.domain.repository

import com.example.classroomseating.domain.model.Classroom
import com.example.classroomseating.domain.model.Desk
import kotlinx.coroutines.flow.Flow

interface ClassroomRepository {

    fun observeAll(): Flow<List<Classroom>>

    fun observeById(id: String): Flow<Classroom?>

    suspend fun getById(id: String): Classroom?

    suspend fun upsert(classroom: Classroom): String

    suspend fun delete(classroom: Classroom)

    fun observeDesks(classroomId: String): Flow<List<Desk>>

    suspend fun replaceDesks(classroomId: String, desks: List<Desk>)

    suspend fun deleteDesk(deskId: String)
}