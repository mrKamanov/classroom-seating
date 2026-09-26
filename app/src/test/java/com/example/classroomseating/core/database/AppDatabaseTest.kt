package com.example.classroomseating.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.classroomseating.core.database.entity.ClassroomEntity
import com.example.classroomseating.core.database.entity.DeskEntity
import com.example.classroomseating.core.database.entity.SchoolClassEntity
import com.example.classroomseating.core.database.entity.SeatingAssignmentEntity
import com.example.classroomseating.core.database.entity.SeatingPlanEntity
import com.example.classroomseating.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

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
    fun migrationFrom1To2ConvertsVisionFlagToRestriction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(V1_STUDENTS_TABLE)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        helper.writableDatabase.use { database ->
            database.execSQL(
                "INSERT INTO students (id, classId, firstName, lastName, gender, " +
                    "visionConstraint, behaviorNote, tagColorHex) " +
                    "VALUES ('s1', 'c1', 'Иван', 'Смирнов', 'MALE', 1, NULL, NULL)"
            )
            database.execSQL(
                "INSERT INTO students (id, classId, firstName, lastName, gender, " +
                    "visionConstraint, behaviorNote, tagColorHex) " +
                    "VALUES ('s2', 'c1', 'Анна', 'Иванова', 'FEMALE', 0, NULL, NULL)"
            )

            AppDatabase.MIGRATION_1_2.migrate(database)

            database.query("SELECT id, restriction FROM students ORDER BY id").use { cursor ->
                cursor.moveToFirst()
                assertEquals("s1", cursor.getString(0))
                assertEquals("VISION", cursor.getString(1))
                cursor.moveToNext()
                assertEquals("s2", cursor.getString(0))
                assertEquals("NONE", cursor.getString(1))
            }
        }
    }

    @Test
    fun cascadeDeleteClassRemovesStudents() = runTest {
        val schoolClass = SchoolClassEntity(name = "5 А", academicYear = "2026-2027")
        db.schoolClassDao().insert(schoolClass)
        db.studentDao().insert(
            StudentEntity(classId = schoolClass.id, firstName = "Иван", lastName = "Смирнов")
        )

        db.schoolClassDao().deleteClassWithData(schoolClass.id)

        assertTrue(db.studentDao().observeByClass(schoolClass.id).first().isEmpty())
        assertTrue(db.schoolClassDao().observeById(schoolClass.id).first() == null)
    }

    @Test
    fun cascadeDeleteClassroomRemovesDesks() = runTest {
        val classroom = ClassroomEntity(name = "Кабинет №304", columnsCount = 3, rowsCount = 3)
        db.classroomDao().insert(classroom)
        db.classroomDao().insertDesks(
            listOf(
                DeskEntity(classroomId = classroom.id, gridX = 0, gridY = 0),
                DeskEntity(classroomId = classroom.id, gridX = 1, gridY = 0)
            )
        )

        db.classroomDao().delete(classroom)

        assertTrue(db.classroomDao().observeDesks(classroom.id).first().isEmpty())
    }

    @Test
    fun fullSeatingPlanRelationReturnsDesksWithSeatedStudents() = runTest {
        val schoolClass = SchoolClassEntity(name = "7 Б", academicYear = "2026-2027")
        db.schoolClassDao().insert(schoolClass)
        val classroom = ClassroomEntity(name = "Кабинет №12", columnsCount = 6, rowsCount = 5)
        db.classroomDao().insert(classroom)
        val desk = DeskEntity(classroomId = classroom.id, gridX = 0, gridY = 0)
        db.classroomDao().insertDesks(listOf(desk))

        val student = StudentEntity(classId = schoolClass.id, firstName = "Пётр", lastName = "Петров")
        db.studentDao().insert(student)

        val plan = SeatingPlanEntity(
            classId = schoolClass.id,
            classroomId = classroom.id,
            title = "Основная"
        )
        db.seatingPlanDao().insertPlan(plan)
        db.seatingPlanDao().insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = plan.id,
                studentId = student.id,
                deskId = desk.id,
                seatIndex = 0
            )
        )

        val full = db.seatingPlanDao().getFullSeatingPlanFlow(plan.id).first()

        assertEquals(plan.id, full?.seatingPlan?.id)
        assertEquals(1, full?.desksWithStudents?.size)
        assertEquals(student.id, full?.desksWithStudents?.first()?.students?.first()?.id)
    }

    @Test
    fun swapStudentsMovesBothAssignments() = runTest {
        val schoolClass = SchoolClassEntity(name = "5 Б", academicYear = "2026-2027")
        db.schoolClassDao().insert(schoolClass)
        val classroom = ClassroomEntity(name = "Кабинет №5", columnsCount = 6, rowsCount = 5)
        db.classroomDao().insert(classroom)
        val desk1 = DeskEntity(classroomId = classroom.id, gridX = 0, gridY = 0)
        val desk2 = DeskEntity(classroomId = classroom.id, gridX = 1, gridY = 0)
        db.classroomDao().insertDesks(listOf(desk1, desk2))

        val student1 = StudentEntity(classId = schoolClass.id, firstName = "Анна", lastName = "Иванова")
        val student2 = StudentEntity(classId = schoolClass.id, firstName = "Олег", lastName = "Сидоров")
        db.studentDao().insertAll(listOf(student1, student2))

        val plan = SeatingPlanEntity(
            classId = schoolClass.id,
            classroomId = classroom.id,
            title = "Основная"
        )
        db.seatingPlanDao().insertPlan(plan)
        db.seatingPlanDao().insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = plan.id,
                studentId = student1.id,
                deskId = desk1.id,
                seatIndex = 0
            )
        )
        db.seatingPlanDao().insertAssignment(
            SeatingAssignmentEntity(
                seatingPlanId = plan.id,
                studentId = student2.id,
                deskId = desk2.id,
                seatIndex = 1
            )
        )

        db.seatingPlanDao().swapStudents(
            planId = plan.id,
            student1Id = student1.id,
            desk1Id = desk1.id,
            seat1Index = 0,
            student2Id = student2.id,
            desk2Id = desk2.id,
            seat2Index = 1
        )

        val assignments = db.seatingPlanDao().getAssignments(plan.id)
        assertEquals(2, assignments.size)
        val a1 = assignments.first { it.studentId == student1.id }
        val a2 = assignments.first { it.studentId == student2.id }
        assertEquals(desk2.id, a1.deskId)
        assertEquals(1, a1.seatIndex)
        assertEquals(desk1.id, a2.deskId)
        assertEquals(0, a2.seatIndex)
    }

    private companion object {
        const val V1_STUDENTS_TABLE = "CREATE TABLE students (" +
            "id TEXT NOT NULL PRIMARY KEY, " +
            "classId TEXT NOT NULL, " +
            "firstName TEXT NOT NULL, " +
            "lastName TEXT NOT NULL, " +
            "gender TEXT NOT NULL, " +
            "visionConstraint INTEGER NOT NULL, " +
            "behaviorNote TEXT, " +
            "tagColorHex TEXT)"
    }
}