package com.example.classroomseating.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.classroomseating.core.database.dao.ClassroomDao
import com.example.classroomseating.core.database.dao.SchoolClassDao
import com.example.classroomseating.core.database.dao.SeatingPlanDao
import com.example.classroomseating.core.database.dao.StudentDao
import com.example.classroomseating.core.database.entity.ClassroomEntity
import com.example.classroomseating.core.database.entity.DeskEntity
import com.example.classroomseating.core.database.entity.SchoolClassEntity
import com.example.classroomseating.core.database.entity.SeatingAssignmentEntity
import com.example.classroomseating.core.database.entity.SeatingPlanEntity
import com.example.classroomseating.core.database.entity.StudentEntity

@Database(
    entities = [
        SchoolClassEntity::class,
        StudentEntity::class,
        ClassroomEntity::class,
        DeskEntity::class,
        SeatingPlanEntity::class,
        SeatingAssignmentEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolClassDao(): SchoolClassDao

    abstract fun studentDao(): StudentDao

    abstract fun classroomDao(): ClassroomDao

    abstract fun seatingPlanDao(): SeatingPlanDao

    companion object {
        const val DATABASE_NAME = "classroom_seating.db"

        /**
         * v1 -> v2: категория ограничения по здоровью вместо галочки «зрение».
         * Старая колонка visionConstraint сохраняется для обратной совместимости.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE students ADD COLUMN restriction TEXT NOT NULL DEFAULT 'NONE'"
                )
                db.execSQL(
                    "UPDATE students SET restriction = 'VISION' WHERE visionConstraint = 1"
                )
            }
        }
    }
}