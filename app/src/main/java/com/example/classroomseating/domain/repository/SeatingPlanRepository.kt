package com.example.classroomseating.domain.repository

import com.example.classroomseating.domain.model.FullSeatingPlan
import com.example.classroomseating.domain.model.SeatingAssignment
import com.example.classroomseating.domain.model.SeatingPlan
import kotlinx.coroutines.flow.Flow

interface SeatingPlanRepository {

    fun observeByClass(classId: String): Flow<List<SeatingPlan>>

    fun observeFullPlan(planId: String): Flow<FullSeatingPlan?>

    fun observeAssignments(planId: String): Flow<List<SeatingAssignment>>

    suspend fun getAssignments(planId: String): List<SeatingAssignment>

    suspend fun clearAssignments(planId: String)

    suspend fun upsertPlan(plan: SeatingPlan): String

    suspend fun assignStudent(planId: String, studentId: String, deskId: String, seatIndex: Int)

    suspend fun unassignStudent(planId: String, studentId: String)

    suspend fun swapStudents(
        planId: String,
        student1: SeatingAssignment,
        student2: SeatingAssignment
    )
}