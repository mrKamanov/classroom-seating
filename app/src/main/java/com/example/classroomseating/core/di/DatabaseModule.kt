package com.example.classroomseating.core.di

import android.content.Context
import androidx.room.Room
import com.example.classroomseating.core.database.AppDatabase
import com.example.classroomseating.core.database.dao.ClassroomDao
import com.example.classroomseating.core.database.dao.SchoolClassDao
import com.example.classroomseating.core.database.dao.SeatingPlanDao
import com.example.classroomseating.core.database.dao.StudentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideSchoolClassDao(db: AppDatabase): SchoolClassDao = db.schoolClassDao()

    @Provides
    fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()

    @Provides
    fun provideClassroomDao(db: AppDatabase): ClassroomDao = db.classroomDao()

    @Provides
    fun provideSeatingPlanDao(db: AppDatabase): SeatingPlanDao = db.seatingPlanDao()
}