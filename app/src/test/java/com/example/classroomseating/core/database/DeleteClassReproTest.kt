package com.example.classroomseating.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.classroomseating.core.database.dao.ClassWithStudentCount
import com.example.classroomseating.core.database.entity.ClassroomEntity
import com.example.classroomseating.core.database.entity.DeskEntity
import com.example.classroomseating.core.database.entity.SchoolClassEntity
import com.example.classroomseating.core.database.entity.SeatingAssignmentEntity
import com.example.classroomseating.core.database.entity.SeatingPlanEntity
import com.example.classroomseating.core.database.entity.StudentEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeleteClassReproTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deletingSchoolClass_removesClassAndAllDependents() = runTest {
        val schoolClass = SchoolClassEntity(name = "9 Л", academicYear = "2026-2027")
        db.schoolClassDao().insert(schoolClass)
        val student = StudentEntity(classId = schoolClass.id, firstName = "Иван", lastName = "Иванов")
        db.studentDao().insert(student)
        val classroom = ClassroomEntity(name = "Кабинет", columnsCount = 3, rowsCount = 3)
        db.classroomDao().insert(classroom)
        val desk = DeskEntity(classroomId = classroom.id, gridX = 0, gridY = 0)
        db.classroomDao().insertDesks(listOf(desk))
        val plan = SeatingPlanEntity(classId = schoolClass.id, classroomId = classroom.id, title = "Основная")
        db.seatingPlanDao().insertPlan(plan)
        db.seatingPlanDao().insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = plan.id,
                studentId = student.id,
                deskId = desk.id,
                seatIndex = 0
            )
        )

        // Тот же вызов, что в DashboardViewModel.deleteClass.
        db.schoolClassDao().deleteClassWithData(schoolClass.id)

        // Класс должен исчезнуть из БД.
        val remainingClass = db.schoolClassDao().getById(schoolClass.id)
        val remainingStudents = db.studentDao().observeByClass(schoolClass.id).first()
        val remainingPlans = db.seatingPlanDao().observeByClass(schoolClass.id).first()
        val remainingAssignments = db.seatingPlanDao().getAssignments(plan.id)

        println("CLASS_REMAIN=${remainingClass != null}")
        println("STUDENTS_REMAIN=${remainingStudents.size}")
        println("PLANS_REMAIN=${remainingPlans.size}")
        println("ASSIGNMENTS_REMAIN=${remainingAssignments.size}")

        assertNull(remainingClass)
        assertTrue(remainingStudents.isEmpty())
        assertTrue(remainingPlans.isEmpty())
        assertTrue(remainingAssignments.isEmpty())
        assertEquals(1, db.classroomDao().observeDesks(classroom.id).first().size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun dashboardViewModelPipeline_emitsDeletionLive() = runBlocking {
        // Одиночный запрос дашборда: observeWithStudentCounts (подзапрос на count).
        val classDao = db.schoolClassDao()
        val studentDao = db.studentDao()

        val kept = SchoolClassEntity(name = "5 Б", academicYear = "2026-2027")
        val removed = SchoolClassEntity(name = "9 Л", academicYear = "2026-2027")
        classDao.insert(kept)
        classDao.insert(removed)

        val pipeline = classDao.observeWithStudentCounts()

        val timeline = mutableListOf<List<ClassWithStudentCount>>()
        val job = launch { pipeline.collect { timeline.add(it) } }
        withTimeout(5_000) { while (timeline.isEmpty()) delay(10) }

        classDao.deleteClassWithData(removed.id)

        withTimeout(5_000) {
            while (timeline.lastOrNull()?.any { it.id == removed.id } != false) delay(10)
        }
        job.cancel()

        println("TIMELINE=$timeline")
        val last = timeline.last()
        assertTrue(last.none { it.id == removed.id })
        assertEquals(listOf(kept.id), last.map { it.id })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insertingNewClass_emitsInDashboardPipelineLive() = runBlocking {
        val classDao = db.schoolClassDao()

        classDao.insert(SchoolClassEntity(name = "5 Б", academicYear = "2026-2027"))
        val pipeline = classDao.observeWithStudentCounts()

        val timeline = mutableListOf<List<ClassWithStudentCount>>()
        val job = launch { pipeline.collect { timeline.add(it) } }
        withTimeout(5_000) { while (timeline.isEmpty()) delay(10) }

        classDao.insert(SchoolClassEntity(name = "9 Л", academicYear = "2026-2027"))

        withTimeout(5_000) {
            while (timeline.lastOrNull()?.size != 2) delay(10)
        }
        job.cancel()

        assertEquals(2, timeline.last().size)
    }
}