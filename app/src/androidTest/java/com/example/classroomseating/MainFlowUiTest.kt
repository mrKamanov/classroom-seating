package com.example.classroomseating

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.classroomseating.core.database.AppDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты основных пользовательских сценариев (4.4):
 * создание класса, добавление учеников, конструктор кабинета,
 * автоматическая рассадка и экран обмена.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainFlowUiTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
        database.clearAllTables()
    }

    @Test
    fun createClass_addStudents_autoArrangeSeating() {
        launchApp()
        createClass(name = "5 А")
        addStudentsManually()
        openLayoutEditorAndSave()
        openSeatingAndAutoArrange()

        composeRule.onNodeWithText("Нерассаженные (0)", substring = true).assertExists()
        composeRule.onNodeWithText("Иванов И.", substring = true).assertExists()
        composeRule.onNodeWithText("Петров П.", substring = true).assertExists()
    }

    @Test
    fun shareScreen_showsSummaryAndQr() {
        launchApp()
        createClass(name = "6 Б")
        addStudentsManually()
        openLayoutEditorAndSave()
        openSeatingAndAutoArrange()
        openShareScreen()

        composeRule.onNodeWithText("Обмен рассадкой").assertExists()
        composeRule.onNodeWithText("Класс: 6 Б", substring = true).assertExists()
        composeRule.onNodeWithText("Учеников: 3", substring = true).assertExists()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasContentDescription("QR-код рассадки"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ---- helpers ----

    private fun launchApp() {
        ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
    }

    private fun createClass(name: String) {
        composeRule.onNodeWithText("Класс").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("Новый класс")).fetchSemanticsNodes().isNotEmpty()
        }
        textFieldByLabel("Название класса").performTextInput(name)
        composeRule.onNodeWithText("Создать").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription("Рассадка"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addStudentsManually() {
        addStudent("Иванов", "Иван")
        addStudent("Петров", "Пётр")
        addStudent("Сидоров", "Сидр")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("Иванов Иван")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addStudent(lastName: String, firstName: String) {
        composeRule.onNodeWithText("Ученик").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("Новый ученик")).fetchSemanticsNodes().isNotEmpty()
        }
        textFieldByLabel("Фамилия").performTextInput(lastName)
        textFieldByLabel("Имя").performTextInput(firstName)
        composeRule.onNodeWithText("Сохранить").performClick()
    }

    private fun openLayoutEditorAndSave() {
        composeRule.onNodeWithContentDescription("Настроить кабинет").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("Конструктор кабинета")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Сгенерировать парты и сохранить").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription("Рассадка"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSeatingAndAutoArrange() {
        composeRule.onNodeWithContentDescription("Рассадка").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("Нерассаженные (3)", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Автоматическая рассадка").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText("Нерассаженные (0)", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openShareScreen() {
        composeRule.onNodeWithContentDescription("Обмен рассадкой").performClick()
        composeRule.waitForIdle()
    }

    private fun textFieldByLabel(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasText(label) and hasSetTextAction())
}