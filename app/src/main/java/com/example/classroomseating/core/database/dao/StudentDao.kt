package com.example.classroomseating.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.classroomseating.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY lastName, firstName")
    fun observeByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE classId = :classId")
    fun observeCount(classId: String): Flow<Int>

    @Query("SELECT * FROM students WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>)

    @Update
    suspend fun update(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteById(studentId: String)

    @Query("DELETE FROM students WHERE classId = :classId")
    suspend fun deleteByClass(classId: String)

    @Query("UPDATE students SET classId = :newClassId WHERE id = :studentId")
    suspend fun moveToClass(studentId: String, newClassId: String)
}