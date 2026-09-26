package com.example.classroomseating.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.classroomseating.core.database.entity.SchoolClassEntity
import kotlinx.coroutines.flow.Flow

/** Строка дашборда: класс + количество учеников (один запрос, без N+1). */
data class ClassWithStudentCount(
    val id: String,
    val name: String,
    val academicYear: String,
    val classTeacherName: String?,
    val studentCount: Int
)

@Dao
interface SchoolClassDao {

    @Query("SELECT * FROM school_classes ORDER BY name")
    fun observeAll(): Flow<List<SchoolClassEntity>>

    @Query(
        """
        SELECT c.id, c.name, c.academicYear, c.classTeacherName,
               (SELECT COUNT(*) FROM students s WHERE s.classId = c.id) AS studentCount
        FROM school_classes c
        ORDER BY c.name
        """
    )
    fun observeWithStudentCounts(): Flow<List<ClassWithStudentCount>>

    @Query("SELECT * FROM school_classes WHERE id = :id")
    fun observeById(id: String): Flow<SchoolClassEntity?>

    @Query("SELECT * FROM school_classes WHERE id = :id")
    suspend fun getById(id: String): SchoolClassEntity?

    @Query("SELECT * FROM school_classes WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SchoolClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schoolClass: SchoolClassEntity): Long

    @Update
    suspend fun update(schoolClass: SchoolClassEntity)

    @Query("DELETE FROM school_classes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM seating_assignments WHERE seatingPlanId IN " +
        "(SELECT id FROM seating_plans WHERE classId = :classId)")
    suspend fun deleteAssignmentsForClass(classId: String)

    @Query("DELETE FROM seating_plans WHERE classId = :classId")
    suspend fun deletePlansForClass(classId: String)

    @Query("DELETE FROM students WHERE classId = :classId")
    suspend fun deleteStudentsForClass(classId: String)

    /**
     * Удаление класса со всеми зависимыми данными в одной транзакции:
     * назначения мест, схемы рассадки, ученики и сам класс.
     */
    @Transaction
    open suspend fun deleteClassWithData(classId: String) {
        deleteAssignmentsForClass(classId)
        deletePlansForClass(classId)
        deleteStudentsForClass(classId)
        deleteById(classId)
    }
}