package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore by preferencesDataStore(name = "nova_settings")

class NovaTypeConverters {
    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority =
        runCatching { TaskPriority.valueOf(value) }.getOrDefault(TaskPriority.MEDIUM)

    @TypeConverter
    fun fromCategory(category: TaskCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): TaskCategory =
        runCatching { TaskCategory.valueOf(value) }.getOrDefault(TaskCategory.OTHER)

    @TypeConverter
    fun fromRecurring(recurring: RecurringType): String = recurring.name

    @TypeConverter
    fun toRecurring(value: String): RecurringType =
        runCatching { RecurringType.valueOf(value) }.getOrDefault(RecurringType.NONE)
}

@Dao
interface NovaTaskDao {
    @Query("SELECT * FROM nova_tasks ORDER BY dateIso ASC, startTime ASC, id ASC")
    fun observeAllTasks(): Flow<List<NovaTask>>

    @Query("SELECT * FROM nova_tasks ORDER BY dateIso ASC, startTime ASC, id ASC")
    suspend fun getAllTasksSnapshot(): List<NovaTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: NovaTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<NovaTask>): List<Long>

    @Update
    suspend fun updateTask(task: NovaTask)

    @Query("DELETE FROM nova_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM nova_tasks WHERE isSample = 1")
    suspend fun deleteSampleTasks()

    @Query("DELETE FROM nova_tasks")
    suspend fun deleteAllTasks()
}

@Database(entities = [NovaTask::class], version = 1, exportSchema = false)
@TypeConverters(NovaTypeConverters::class)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun taskDao(): NovaTaskDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_life_manager.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class NovaRepository(
    private val taskDao: NovaTaskDao,
    private val appContext: Context
) {
    private object PrefKeys {
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_PLAN_START = stringPreferencesKey("default_plan_start")
        val WORK_HOURS_START = stringPreferencesKey("work_hours_start")
        val WORK_HOURS_END = stringPreferencesKey("work_hours_end")
        val REMINDER_LEAD_MINS = intPreferencesKey("reminder_lead_mins")
    }

    val allTasksFlow: Flow<List<NovaTask>> = taskDao.observeAllTasks()

    val settingsFlow: Flow<UserSettings> = appContext.novaDataStore.data.map { prefs ->
        val langCode = prefs[PrefKeys.LANGUAGE] ?: AppLanguage.HINGLISH.code
        val lang = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.HINGLISH
        UserSettings(
            language = lang,
            darkTheme = prefs[PrefKeys.DARK_THEME] ?: true,
            notificationsEnabled = prefs[PrefKeys.NOTIFICATIONS_ENABLED] ?: false,
            defaultPlanningStartTime = prefs[PrefKeys.DEFAULT_PLAN_START] ?: "07:00",
            workingHoursStart = prefs[PrefKeys.WORK_HOURS_START] ?: "09:00",
            workingHoursEnd = prefs[PrefKeys.WORK_HOURS_END] ?: "21:00",
            reminderLeadMinutes = prefs[PrefKeys.REMINDER_LEAD_MINS] ?: 10
        )
    }

    suspend fun getAllTasksSnapshot(): List<NovaTask> = taskDao.getAllTasksSnapshot()

    suspend fun insertTask(task: NovaTask): Long = taskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<NovaTask>): List<Long> = taskDao.insertTasks(tasks)

    suspend fun updateTask(task: NovaTask) = taskDao.updateTask(task)

    suspend fun deleteTaskById(taskId: Long) = taskDao.deleteTaskById(taskId)

    suspend fun deleteSampleTasks() = taskDao.deleteSampleTasks()

    suspend fun clearAllLocalData() {
        taskDao.deleteAllTasks()
    }

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        appContext.novaDataStore.edit { prefs ->
            val currentLangCode = prefs[PrefKeys.LANGUAGE] ?: AppLanguage.HINGLISH.code
            val current = UserSettings(
                language = AppLanguage.entries.find { it.code == currentLangCode } ?: AppLanguage.HINGLISH,
                darkTheme = prefs[PrefKeys.DARK_THEME] ?: true,
                notificationsEnabled = prefs[PrefKeys.NOTIFICATIONS_ENABLED] ?: false,
                defaultPlanningStartTime = prefs[PrefKeys.DEFAULT_PLAN_START] ?: "07:00",
                workingHoursStart = prefs[PrefKeys.WORK_HOURS_START] ?: "09:00",
                workingHoursEnd = prefs[PrefKeys.WORK_HOURS_END] ?: "21:00",
                reminderLeadMinutes = prefs[PrefKeys.REMINDER_LEAD_MINS] ?: 10
            )
            val updated = transform(current)
            prefs[PrefKeys.LANGUAGE] = updated.language.code
            prefs[PrefKeys.DARK_THEME] = updated.darkTheme
            prefs[PrefKeys.NOTIFICATIONS_ENABLED] = updated.notificationsEnabled
            prefs[PrefKeys.DEFAULT_PLAN_START] = updated.defaultPlanningStartTime
            prefs[PrefKeys.WORK_HOURS_START] = updated.workingHoursStart
            prefs[PrefKeys.WORK_HOURS_END] = updated.workingHoursEnd
            prefs[PrefKeys.REMINDER_LEAD_MINS] = updated.reminderLeadMinutes
        }
    }

    fun buildSampleTasks(todayIso: String = TimeUtils.todayIso()): List<NovaTask> {
        return listOf(
            NovaTask(
                title = "Morning Routine & Hydration",
                description = "Sample task: Wake up fresh, stretch for 10 mins, and review today's top 3 goals.",
                dateIso = todayIso,
                startTime = "07:00",
                durationMinutes = 45,
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.HEALTH,
                hasReminder = true,
                reminderTime = "06:55",
                recurring = RecurringType.DAILY,
                isSample = true
            ),
            NovaTask(
                title = "Deep Study Session",
                description = "Sample task: Focused study block without phone distractions.",
                dateIso = todayIso,
                startTime = "09:00",
                durationMinutes = 120,
                priority = TaskPriority.HIGH,
                category = TaskCategory.STUDY,
                hasReminder = true,
                reminderTime = "08:50",
                recurring = RecurringType.DAILY,
                isSample = true
            ),
            NovaTask(
                title = "Mindful Lunch Break",
                description = "Sample task: Nutritious meal and short walk away from screens.",
                dateIso = todayIso,
                startTime = "13:00",
                durationMinutes = 45,
                priority = TaskPriority.LOW,
                category = TaskCategory.HEALTH,
                hasReminder = false,
                isSample = true
            ),
            NovaTask(
                title = "Work on Business / Projects",
                description = "Sample task: Client deliverables, content creation, and growth tasks.",
                dateIso = todayIso,
                startTime = "17:00",
                durationMinutes = 90,
                priority = TaskPriority.HIGH,
                category = TaskCategory.BUSINESS,
                hasReminder = true,
                reminderTime = "16:50",
                isSample = true
            ),
            NovaTask(
                title = "Evening Exercise & Fitness",
                description = "Sample task: 45-minute workout or brisk outdoor run.",
                dateIso = todayIso,
                startTime = "19:00",
                durationMinutes = 45,
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.FITNESS,
                hasReminder = true,
                reminderTime = "18:50",
                recurring = RecurringType.DAILY,
                isSample = true
            )
        )
    }
}
