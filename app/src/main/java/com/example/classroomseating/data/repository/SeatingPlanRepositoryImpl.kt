package com.example.classroomseating.data.repository

import com.example.classroomseating.core.database.dao.SeatingPlanDao
import com.example.classroomseating.core.database.entity.SeatingAssignmentEntity
import com.example.classroomseating.data.mapper.toDomain
import com.example.classroomseating.data.mapper.toEntity
import com.example.classroomseating.domain.model.FullSeatingPlan
import com.example.classroomseating.domain.model.SeatingAssignment
import com.example.classroomseating.domain.model.SeatingPlan
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SeatingPlanRepositoryImpl @Inject constructor(
    private val dao: SeatingPlanDao
) : SeatingPlanRepository {

    override fun observeByClass(classId: String): Flow<List<SeatingPlan>> =
        dao.observeByClass(classId).map { list -> list.map { it.toDomain() } }

    override fun observeFullPlan(planId: String): Flow<FullSeatingPlan?> =
        dao.getFullSeatingPlanFlow(planId).map { it?.toDomain() }

    override fun observeAssignments(planId: String): Flow<List<SeatingAssignment>> =
        dao.observeAssignments(planId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAssignments(planId: String): List<SeatingAssignment> =
        dao.getAssignments(planId).map { it.toDomain() }

    override suspend fun clearAssignments(planId: String) {
        dao.clearAssignments(planId)
    }

    override suspend fun upsertPlan(plan: SeatingPlan): String {
        dao.insertPlan(plan.toEntity())
        return plan.id
    }

    override suspend fun assignStudent(
        planId: String,
        studentId: String,
        deskId: String,
        seatIndex: Int
    ) {
        dao.insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = planId,
                studentId = studentId,
                deskId = deskId,
                seatIndex = seatIndex
            )
        )
    }

    override suspend fun unassignStudent(planId: String, studentId: String) {
        dao.removeStudentFromPlan(planId, studentId)
    }

    override suspend fun swapStudents(
        planId: String,
        student1: SeatingAssignment,
        student2: SeatingAssignment
    ) {
        dao.swapStudents(
            planId = planId,
            student1Id = student1.studentId,
            desk1Id = student1.deskId,
            seat1Index = student1.seatIndex,
            student2Id = student2.studentId,
            desk2Id = student2.deskId,
            seat2Index = student2.seatIndex
        )
    }
}