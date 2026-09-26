package com.example.classroomseating.data.di

import com.example.classroomseating.data.repository.ClassroomRepositoryImpl
import com.example.classroomseating.data.repository.SchoolClassRepositoryImpl
import com.example.classroomseating.data.repository.SeatingPlanRepositoryImpl
import com.example.classroomseating.data.repository.StudentRepositoryImpl
import com.example.classroomseating.domain.repository.ClassroomRepository
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import com.example.classroomseating.domain.repository.StudentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSchoolClassRepository(impl: SchoolClassRepositoryImpl): SchoolClassRepository

    @Binds
    @Singleton
    abstract fun bindStudentRepository(impl: StudentRepositoryImpl): StudentRepository

    @Binds
    @Singleton
    abstract fun bindClassroomRepository(impl: ClassroomRepositoryImpl): ClassroomRepository

    @Binds
    @Singleton
    abstract fun bindSeatingPlanRepository(impl: SeatingPlanRepositoryImpl): SeatingPlanRepository
}