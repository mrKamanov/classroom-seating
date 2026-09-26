package com.example.classroomseating.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.classroomseating.core.database.entity.FullSeatingPlanRelation
import com.example.classroomseating.core.database.entity.SeatingAssignmentEntity
import com.example.classroomseating.core.database.entity.SeatingPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SeatingPlanDao {

    @Transaction
    @Query("SELECT * FROM seating_plans WHERE id = :planId")
    abstract fun getFullSeatingPlanFlow(planId: String): Flow<FullSeatingPlanRelation?>

    @Query("SELECT * FROM seating_plans WHERE classId = :classId ORDER BY updatedAt DESC")
    abstract fun observeByClass(classId: String): Flow<List<SeatingPlanEntity>>

    @Query("SELECT * FROM seating_plans WHERE classId = :classId ORDER BY updatedAt DESC")
    abstract suspend fun getByClass(classId: String): List<SeatingPlanEntity>

    @Query("SELECT * FROM seating_plans WHERE id = :planId")
    abstract suspend fun getById(planId: String): SeatingPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPlan(plan: SeatingPlanEntity): Long

    @Query("SELECT * FROM seating_assignments WHERE seatingPlanId = :planId")
    abstract suspend fun getAssignments(planId: String): List<SeatingAssignmentEntity>

    @Query("SELECT * FROM seating_assignments WHERE seatingPlanId = :planId")
    abstract fun observeAssignments(planId: String): Flow<List<SeatingAssignmentEntity>>

    @Query("DELETE FROM seating_assignments WHERE seatingPlanId = :planId")
    abstract suspend fun clearAssignments(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssignment(assignment: SeatingAssignmentEntity)

    @Query("DELETE FROM seating_assignments WHERE seatingPlanId = :planId AND studentId = :studentId")
    abstract suspend fun removeStudentFromPlan(planId: String, studentId: String)

    /**
     * Обмен двух учеников местами в рамках одной схемы рассадки.
     */
    @Transaction
    open suspend fun swapStudents(
        planId: String,
        student1Id: String,
        desk1Id: String,
        seat1Index: Int,
        student2Id: String,
        desk2Id: String,
        seat2Index: Int
    ) {
        removeStudentFromPlan(planId, student1Id)
        removeStudentFromPlan(planId, student2Id)

        insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = planId,
                studentId = student1Id,
                deskId = desk2Id,
                seatIndex = seat2Index
            )
        )
        insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = planId,
                studentId = student2Id,
                deskId = desk1Id,
                seatIndex = seat1Index
            )
        )
    }
}