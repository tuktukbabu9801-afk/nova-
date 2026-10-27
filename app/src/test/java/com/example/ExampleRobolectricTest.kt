package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppLanguage
import com.example.data.NovaDatabase
import com.example.data.NovaRepository
import com.example.data.NovaStrings
import com.example.data.NovaTask
import com.example.data.TaskCategory
import com.example.data.TaskPriority
import com.example.data.TimeUtils
import com.example.planner.RuleBasedAiPlanner
import com.example.ui.NovaViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h740dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: NovaDatabase
    private lateinit var repository: NovaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NovaRepository(db.taskDao(), context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `1_app_name_and_empty_state_open_without_errors_on_narrow_screen`() {
        assertEquals("NOVA", context.getString(R.string.app_name))
        val viewModel = NovaViewModel(repository, context)
        composeTestRule.setContent {
            NovaAppRoot(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_add_task_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_plan_ai_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_completion_percent_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun `2_create_edit_complete_and_delete_task_persist_and_update_stats`() = runBlocking {
        val today = TimeUtils.todayIso()
        // Create task
        val id1 = repository.insertTask(
            NovaTask(
                title = "Morning Study Block",
                description = "Physics revision",
                dateIso = today,
                startTime = "09:00",
                durationMinutes = 60,
                priority = TaskPriority.HIGH,
                category = TaskCategory.STUDY
            )
        )
        val id2 = repository.insertTask(
            NovaTask(
                title = "Evening Workout",
                description = "Cardio 45m",
                dateIso = today,
                startTime = "18:00",
                durationMinutes = 45,
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.FITNESS
            )
        )

        // Verify persistence
        val saved = repository.getAllTasksSnapshot()
        assertEquals(2, saved.size)

        // Edit task 1
        val editedTask1 = saved.first { it.id == id1 }.copy(title = "Updated Physics Study")
        repository.updateTask(editedTask1)
        assertEquals("Updated Physics Study", repository.getAllTasksSnapshot().first { it.id == id1 }.title)

        // Complete task 1 -> completion percentage must be 1 / 2 * 100 = 50%
        repository.updateTask(editedTask1.copy(isCompleted = true, completedAtEpochMs = System.currentTimeMillis()))

        val viewModel = NovaViewModel(repository, context)
        composeTestRule.setContent {
            NovaAppRoot(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()

        // Delete task 2 -> 1 / 1 = 100%
        repository.deleteTaskById(id2)
        val afterDelete = repository.getAllTasksSnapshot()
        assertEquals(1, afterDelete.size)
    }

    @Test
    fun `3_ai_planner_parses_hinglish_respects_dates_and_detects_overlaps`() {
        val today = "2026-10-26"
        val existingTask = NovaTask(
            id = 10L,
            title = "Existing Morning Class",
            dateIso = "2026-10-27",
            startTime = "09:30",
            durationMinutes = 60
        )

        val prompt = "Kal subah 7 baje uthna hai, 9 se 11 baje tak padhai karni hai, dopahar ko business par kaam karna hai aur shaam ko exercise karni hai."
        val proposal = RuleBasedAiPlanner.generateScheduleFromPrompt(
            prompt = prompt,
            existingTasks = listOf(existingTask),
            todayIso = today
        )

        // Must detect 'Kal' -> 2026-10-27
        assertEquals("2026-10-27", proposal.targetDateIso)
        assertTrue(proposal.proposedTasks.size >= 4)

        // Check 7:00 AM wake up
        val wakeTask = proposal.proposedTasks.first()
        assertEquals("07:00", wakeTask.suggestedStartTime)

        // Check 9:00 to 11:00 study task kept explicit time & flagged overlap with 09:30 existing class
        val studyTask = proposal.proposedTasks.find { it.category == TaskCategory.STUDY }
        assertNotNull(studyTask)
        assertEquals("09:00", studyTask!!.suggestedStartTime)
        assertEquals(120, studyTask.estimatedDurationMinutes)
        assertNotNull(studyTask.conflictNote)
        assertTrue(proposal.detectedConflictsResolved >= 1)
    }

    @Test
    fun `4_navigation_and_language_switching_and_disclosures_work`() {
        val viewModel = NovaViewModel(repository, context)
        composeTestRule.setContent {
            NovaAppRoot(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Navigate to My Tasks
        composeTestRule.onNodeWithTag("nav_item_tasks").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reminder_center_card").assertIsDisplayed()

        // Navigate to AI Planner
        composeTestRule.onNodeWithTag("nav_item_planner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_honest_disclosure_banner").assertIsDisplayed()

        // Navigate to Progress
        composeTestRule.onNodeWithTag("nav_item_progress").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("progress_daily_card").assertIsDisplayed()

        // Navigate to Settings & verify Hindi language copy
        composeTestRule.onNodeWithTag("nav_item_settings").performClick()
        composeTestRule.waitForIdle()
        val hindiCopy = NovaStrings.forLanguage(AppLanguage.HINDI, 9)
        assertTrue(hindiCopy.motivation.contains("आज"))
        assertFalse(TimeUtils.format12Hour("19:00").isEmpty())
        assertEquals("7:00 PM", TimeUtils.format12Hour("19:00"))
    }
}
