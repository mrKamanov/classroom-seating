package com.example.classroomseating.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.classroomseating.core.database.entity.ClassroomEntity
import com.example.classroomseating.core.database.entity.DeskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {

    @Query("SELECT * FROM classrooms ORDER BY name")
    fun observeAll(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE id = :id")
    fun observeById(id: String): Flow<ClassroomEntity?>

    @Query("SELECT * FROM classrooms WHERE id = :id")
    suspend fun getById(id: String): ClassroomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(classroom: ClassroomEntity): Long

    @Update
    suspend fun update(classroom: ClassroomEntity)

    @Delete
    suspend fun delete(classroom: ClassroomEntity)

    @Query("SELECT * FROM desks WHERE classroomId = :classroomId ORDER BY gridY, gridX")
    fun observeDesks(classroomId: String): Flow<List<DeskEntity>>

    @Query("SELECT * FROM desks WHERE classroomId = :classroomId ORDER BY gridY, gridX")
    suspend fun getDesks(classroomId: String): List<DeskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesks(desks: List<DeskEntity>)

    @Query("DELETE FROM desks WHERE classroomId = :classroomId")
    suspend fun deleteDesks(classroomId: String)

    @Query("DELETE FROM desks WHERE id = :deskId")
    suspend fun deleteDesk(deskId: String)
}