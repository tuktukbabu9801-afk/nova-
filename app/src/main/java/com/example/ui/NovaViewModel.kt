package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.NovaRepository
import com.example.data.NovaTask
import com.example.data.RecurringType
import com.example.data.TaskCategory
import com.example.data.TaskFilter
import com.example.data.TaskPriority
import com.example.data.TimeUtils
import com.example.data.UserSettings
import com.example.notifications.TaskReminderScheduler
import com.example.planner.AiPlanProposal
import com.example.planner.ProposedTaskItem
import com.example.planner.RuleBasedAiPlanner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DailyBarStat(
    val dateIso: String,
    val dayShortLabel: String,
    val totalTasks: Int,
    val completedTasks: Int
) {
    val percentage: Int
        get() = if (totalTasks > 0) ((completedTasks * 100f) / totalTasks).toInt() else 0
}

data class NovaUiState(
    val isLoading: Boolean = false,
    val allTasks: List<NovaTask> = emptyList(),
    val todayTasks: List<NovaTask> = emptyList(),
    val todayTotal: Int = 0,
    val todayCompleted: Int = 0,
    val todayPending: Int = 0,
    val todayCompletionPercent: Int = 0,
    val nextUpcomingTask: NovaTask? = null,
    val overdueTasks: List<NovaTask> = emptyList(),
    val upcomingTasks: List<NovaTask> = emptyList(),
    val activeReminderTasks: List<NovaTask> = emptyList(),
    val suggestedRescheduleTask: NovaTask? = null,
    val suggestedRescheduleSlot: String = "19:00",
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategoryFilter: TaskCategory? = null,
    val hasSampleTasks: Boolean = false,
    val weeklyStats: List<DailyBarStat> = emptyList(),
    val currentStreakDays: Int = 0,
    val hasStreakHistory: Boolean = false,
    val mostProductiveDayLabel: String? = null,
    val settings: UserSettings = UserSettings(),
    val aiProposal: AiPlanProposal? = null,
    val aiPromptText: String = "",
    val snackbarMessage: String? = null
)

private data class LocalInteractionState(
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategoryFilter: TaskCategory? = null,
    val aiProposal: AiPlanProposal? = null,
    val aiPromptText: String = "",
    val regenerationCount: Int = 0,
    val dismissedRescheduleTaskIds: Set<Long> = emptySet(),
    val snackbarMessage: String? = null
)

class NovaViewModel(
    private val repository: NovaRepository,
    private val appContext: Context
) : ViewModel() {

    private val localState = MutableStateFlow(LocalInteractionState())

    val uiState: StateFlow<NovaUiState> = combine(
        repository.allTasksFlow,
        repository.settingsFlow,
        localState
    ) { tasks, settings, local ->
        val todayIso = TimeUtils.todayIso()
        val nowMins = TimeUtils.currentMinutesOfDay()

        val sortedAll = tasks.sortedWith(
            compareBy<NovaTask> { it.dateIso }
                .thenBy { it.startMinutesOfDay() }
                .thenByDescending { it.priority.weight }
        )

        val todayTasks = sortedAll.filter { it.dateIso == todayIso }
        val todayTotal = todayTasks.size
        val todayCompleted = todayTasks.count { it.isCompleted }
        val todayPending = todayTotal - todayCompleted
        val todayPercent = if (todayTotal > 0) {
            ((todayCompleted * 100.0) / todayTotal).toInt()
        } else {
            0
        }

        val nextUpcoming = todayTasks
            .filter { !it.isCompleted && it.endMinutesOfDay() >= nowMins }
            .minByOrNull { it.startMinutesOfDay() }
            ?: sortedAll.firstOrNull { !it.isCompleted && it.dateIso > todayIso }

        val overdue = sortedAll.filter { it.isOverdue(todayIso, nowMins) }
        val upcoming = sortedAll.filter {
            !it.isCompleted && (it.dateIso > todayIso || (it.dateIso == todayIso && it.startMinutesOfDay() >= nowMins))
        }
        val reminderTasks = sortedAll.filter { it.hasReminder && !it.isCompleted }

        // Smart Rescheduling candidate: first pending task today that is overdue or due soon & not dismissed
        val rescheduleCandidate = todayTasks.firstOrNull {
            !it.isCompleted && it.id !in local.dismissedRescheduleTaskIds &&
                (it.isOverdue(todayIso, nowMins) || it.startMinutesOfDay() <= nowMins)
        } ?: todayTasks.firstOrNull {
            !it.isCompleted && it.id !in local.dismissedRescheduleTaskIds
        }

        val suggestedSlot = if (rescheduleCandidate != null) {
            RuleBasedAiPlanner.suggestRescheduleSlot(rescheduleCandidate, todayTasks, nowMins)
        } else {
            "19:00"
        }

        // Weekly stats & streak calculated ONLY from non-sample tasks (unless only sample tasks exist, labeled clearly)
        val hasSamples = sortedAll.any { it.isSample }
        val statsSourceTasks = if (sortedAll.any { !it.isSample }) {
            sortedAll.filter { !it.isSample }
        } else {
            sortedAll
        }

        val todayDate = LocalDate.parse(todayIso)
        val weekly = (6 downTo 0).map { offset ->
            val d = todayDate.minusDays(offset.toLong())
            val dIso = d.toString()
            val dayTasks = statsSourceTasks.filter { it.dateIso == dIso }
            DailyBarStat(
                dateIso = dIso,
                dayShortLabel = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                totalTasks = dayTasks.size,
                completedTasks = dayTasks.count { it.isCompleted }
            )
        }

        // Calculate real activity streak from actual saved non-sample completions
        val realCompletedTasks = sortedAll.filter { !it.isSample && it.isCompleted }
        val completedDates = realCompletedTasks.map { it.dateIso }.toSet()
        var streak = 0
        var cursorDate = if (todayIso in completedDates) todayDate else todayDate.minusDays(1)
        while (cursorDate.toString() in completedDates) {
            streak++
            cursorDate = cursorDate.minusDays(1)
        }

        // Most productive day of week (only when >= 2 completed tasks exist)
        val productiveDay: String? = if (realCompletedTasks.size >= 2) {
            realCompletedTasks
                .mapNotNull { runCatching { LocalDate.parse(it.dateIso).dayOfWeek }.getOrNull() }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        } else {
            null
        }

        NovaUiState(
            isLoading = false,
            allTasks = sortedAll,
            todayTasks = todayTasks,
            todayTotal = todayTotal,
            todayCompleted = todayCompleted,
            todayPending = todayPending,
            todayCompletionPercent = todayPercent,
            nextUpcomingTask = nextUpcoming,
            overdueTasks = overdue,
            upcomingTasks = upcoming,
            activeReminderTasks = reminderTasks,
            suggestedRescheduleTask = rescheduleCandidate,
            suggestedRescheduleSlot = suggestedSlot,
            selectedFilter = local.selectedFilter,
            selectedCategoryFilter = local.selectedCategoryFilter,
            hasSampleTasks = hasSamples,
            weeklyStats = weekly,
            currentStreakDays = streak,
            hasStreakHistory = realCompletedTasks.isNotEmpty(),
            mostProductiveDayLabel = productiveDay,
            settings = settings,
            aiProposal = local.aiProposal,
            aiPromptText = local.aiPromptText,
            snackbarMessage = local.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NovaUiState(isLoading = true)
    )

    fun setTaskFilter(filter: TaskFilter) {
        localState.update { it.copy(selectedFilter = filter) }
    }

    fun setCategoryFilter(category: TaskCategory?) {
        localState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun clearSnackbar() {
        localState.update { it.copy(snackbarMessage = null) }
    }

    private fun postMessage(msg: String) {
        localState.update { it.copy(snackbarMessage = msg) }
    }

    fun saveTask(
        existingId: Long = 0,
        title: String,
        description: String,
        dateIso: String,
        startTime: String,
        durationMinutes: Int,
        priority: TaskPriority,
        category: TaskCategory,
        hasReminder: Boolean,
        reminderTime: String,
        recurring: RecurringType
    ): String? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return "Task title cannot be empty."
        if (!TimeUtils.isValidIsoDate(dateIso)) return "Please enter a valid date (YYYY-MM-DD)."
        if (!TimeUtils.isValidHhMm(startTime)) return "Please enter a valid 24h time (HH:MM)."

        val validDuration = durationMinutes.coerceIn(5, 720)
        val effectiveReminderTime = if (hasReminder && TimeUtils.isValidHhMm(reminderTime)) {
            reminderTime.trim()
        } else if (hasReminder) {
            val lead = uiState.value.settings.reminderLeadMinutes
            TimeUtils.minutesToHhMm((TimeUtils.parseMinutesOfDay(startTime) - lead).coerceAtLeast(0))
        } else {
            ""
        }

        viewModelScope.launch {
            val currentTasks = repository.getAllTasksSnapshot()
            val existingTask = currentTasks.find { it.id == existingId }
            val taskToSave = NovaTask(
                id = existingId,
                title = cleanTitle,
                description = description.trim(),
                dateIso = dateIso.trim(),
                startTime = startTime.trim(),
                durationMinutes = validDuration,
                priority = priority,
                category = category,
                isCompleted = existingTask?.isCompleted ?: false,
                completedAtEpochMs = existingTask?.completedAtEpochMs,
                hasReminder = hasReminder,
                reminderTime = effectiveReminderTime,
                recurring = recurring,
                isSample = false // Editing or creating makes it a real user task
            )

            val savedId = if (existingId == 0L) {
                repository.insertTask(taskToSave)
            } else {
                repository.updateTask(taskToSave)
                existingId
            }

            val savedTask = taskToSave.copy(id = savedId)
            if (savedTask.hasReminder && uiState.value.settings.notificationsEnabled) {
                TaskReminderScheduler.scheduleTaskReminder(appContext, savedTask)
            } else {
                TaskReminderScheduler.cancelTaskReminder(appContext, savedId)
            }

            postMessage(if (existingId == 0L) "Task '${cleanTitle}' created!" else "Task '${cleanTitle}' updated!")
        }
        return null
    }

    fun toggleTaskCompletion(task: NovaTask) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            val updated = task.copy(
                isCompleted = nowCompleted,
                completedAtEpochMs = if (nowCompleted) System.currentTimeMillis() else null,
                snoozedUntilTime = null
            )
            repository.updateTask(updated)
            if (nowCompleted) {
                TaskReminderScheduler.cancelTaskReminder(appContext, task.id)
                postMessage("Completed: ${task.title}")
            } else {
                if (updated.hasReminder && uiState.value.settings.notificationsEnabled) {
                    TaskReminderScheduler.scheduleTaskReminder(appContext, updated)
                }
                postMessage("Reopened: ${task.title}")
            }
        }
    }

    fun deleteTask(task: NovaTask) {
        viewModelScope.launch {
            TaskReminderScheduler.cancelTaskReminder(appContext, task.id)
            repository.deleteTaskById(task.id)
            postMessage("Deleted '${task.title}'.")
        }
    }

    fun rescheduleTask(task: NovaTask, newDateIso: String, newStartTime: String) {
        if (!TimeUtils.isValidIsoDate(newDateIso) || !TimeUtils.isValidHhMm(newStartTime)) {
            postMessage("Invalid date or time for rescheduling.")
            return
        }
        viewModelScope.launch {
            val lead = uiState.value.settings.reminderLeadMinutes
            val newReminderTime = if (task.hasReminder) {
                TimeUtils.minutesToHhMm((TimeUtils.parseMinutesOfDay(newStartTime) - lead).coerceAtLeast(0))
            } else {
                task.reminderTime
            }
            val updated = task.copy(
                dateIso = newDateIso,
                startTime = newStartTime,
                reminderTime = newReminderTime,
                snoozedUntilTime = null
            )
            repository.updateTask(updated)
            if (updated.hasReminder && uiState.value.settings.notificationsEnabled) {
                TaskReminderScheduler.scheduleTaskReminder(appContext, updated)
            }
            postMessage("Rescheduled '${task.title}' to ${TimeUtils.format12Hour(newStartTime)} ($newDateIso).")
        }
    }

    fun snoozeTaskReminder(task: NovaTask, snoozeMinutes: Int = 15) {
        viewModelScope.launch {
            val baseMins = maxOf(TimeUtils.currentMinutesOfDay(), TimeUtils.parseMinutesOfDay(task.startTime))
            val snoozedHhMm = TimeUtils.minutesToHhMm((baseMins + snoozeMinutes).coerceAtMost(23 * 60 + 55))
            val updated = task.copy(
                hasReminder = true,
                snoozedUntilTime = snoozedHhMm
            )
            repository.updateTask(updated)
            if (uiState.value.settings.notificationsEnabled) {
                TaskReminderScheduler.scheduleTaskReminder(appContext, updated)
            }
            postMessage("Snoozed '${task.title}' reminder until ${TimeUtils.format12Hour(snoozedHhMm)}.")
        }
    }

    fun keepTaskPending(taskId: Long) {
        localState.update { state ->
            state.copy(
                dismissedRescheduleTaskIds = state.dismissedRescheduleTaskIds + taskId,
                snackbarMessage = "Kept task as pending in your list."
            )
        }
    }

    fun loadSampleTasks() {
        viewModelScope.launch {
            repository.deleteSampleTasks()
            val samples = repository.buildSampleTasks(TimeUtils.todayIso())
            repository.insertTasks(samples)
            postMessage("Loaded 5 sample tasks for today!")
        }
    }

    fun removeSampleTasks() {
        viewModelScope.launch {
            repository.deleteSampleTasks()
            postMessage("Removed all sample tasks. Showing only your personal data.")
        }
    }

    // --- AI Planner Actions ---
    fun updateAiPromptText(text: String) {
        localState.update { it.copy(aiPromptText = text) }
    }

    fun generateAiPlan(promptOverride: String? = null) {
        val prompt = (promptOverride ?: localState.value.aiPromptText).trim()
        if (prompt.isEmpty()) {
            postMessage("Please describe your tasks or goals first.")
            return
        }
        val state = uiState.value
        val proposal = RuleBasedAiPlanner.generateScheduleFromPrompt(
            prompt = prompt,
            existingTasks = state.allTasks,
            settings = state.settings,
            todayIso = TimeUtils.todayIso(),
            regenerationSeed = 0
        )
        localState.update {
            it.copy(
                aiPromptText = prompt,
                aiProposal = proposal,
                regenerationCount = 0
            )
        }
    }

    fun regenerateAiPlan() {
        val currentLocal = localState.value
        val prompt = currentLocal.aiPromptText.trim()
        if (prompt.isEmpty()) return
        val nextSeed = currentLocal.regenerationCount + 1
        val state = uiState.value
        val proposal = RuleBasedAiPlanner.generateScheduleFromPrompt(
            prompt = prompt,
            existingTasks = state.allTasks,
            settings = state.settings,
            todayIso = TimeUtils.todayIso(),
            regenerationSeed = nextSeed
        )
        localState.update {
            it.copy(
                aiProposal = proposal,
                regenerationCount = nextSeed,
                snackbarMessage = "Regenerated alternative schedule slots!"
            )
        }
    }

    fun updateProposedTaskItem(updatedItem: ProposedTaskItem) {
        localState.update { current ->
            val prop = current.aiProposal ?: return@update current
            val updatedList = prop.proposedTasks
                .map { if (it.id == updatedItem.id) updatedItem else it }
                .sortedBy { TimeUtils.parseMinutesOfDay(it.suggestedStartTime) }
            current.copy(aiProposal = prop.copy(proposedTasks = updatedList))
        }
    }

    fun removeProposedTaskItem(itemId: Int) {
        localState.update { current ->
            val prop = current.aiProposal ?: return@update current
            current.copy(
                aiProposal = prop.copy(
                    proposedTasks = prop.proposedTasks.filterNot { it.id == itemId }
                )
            )
        }
    }

    fun cancelAiPlan() {
        localState.update { it.copy(aiProposal = null) }
    }

    fun confirmAndSaveAiPlan() {
        val proposal = localState.value.aiProposal ?: return
        if (proposal.proposedTasks.isEmpty()) {
            postMessage("No tasks in the proposed schedule to save.")
            return
        }
        viewModelScope.launch {
            val lead = uiState.value.settings.reminderLeadMinutes
            val tasksToInsert = proposal.proposedTasks.map { item ->
                val startMins = TimeUtils.parseMinutesOfDay(item.suggestedStartTime)
                NovaTask(
                    title = item.title,
                    description = item.description,
                    dateIso = item.suggestedDateIso,
                    startTime = item.suggestedStartTime,
                    durationMinutes = item.estimatedDurationMinutes,
                    priority = item.priority,
                    category = item.category,
                    hasReminder = !item.isBreak,
                    reminderTime = TimeUtils.minutesToHhMm((startMins - lead).coerceAtLeast(0)),
                    isSample = false
                )
            }
            val ids = repository.insertTasks(tasksToInsert)
            if (uiState.value.settings.notificationsEnabled) {
                tasksToInsert.zip(ids).forEach { (task, id) ->
                    TaskReminderScheduler.scheduleTaskReminder(appContext, task.copy(id = id))
                }
            }
            localState.update {
                it.copy(
                    aiProposal = null,
                    aiPromptText = "",
                    snackbarMessage = "Saved ${tasksToInsert.size} scheduled tasks for ${proposal.targetDateIso}!"
                )
            }
        }
    }

    // --- Night Planning ("Plan Tomorrow") ---
    fun moveSelectedTasksToTomorrow(selectedTaskIds: Set<Long>) {
        if (selectedTaskIds.isEmpty()) {
            postMessage("Select at least one unfinished task to move to tomorrow.")
            return
        }
        viewModelScope.launch {
            val tomorrow = TimeUtils.tomorrowIso()
            val all = repository.getAllTasksSnapshot()
            val tomorrowExisting = all.filter { it.dateIso == tomorrow && !it.isCompleted }.toMutableList()
            var movedCount = 0

            for (task in all.filter { it.id in selectedTaskIds && !it.isCompleted }) {
                var targetStartMins = task.startMinutesOfDay()
                while (tomorrowExisting.any { it.overlapsWith(tomorrow, targetStartMins, task.durationMinutes) } &&
                    targetStartMins + task.durationMinutes <= 23 * 60
                ) {
                    targetStartMins += 30
                }
                val newStartHhMm = TimeUtils.minutesToHhMm(targetStartMins)
                val updated = task.copy(
                    dateIso = tomorrow,
                    startTime = newStartHhMm,
                    snoozedUntilTime = null
                )
                repository.updateTask(updated)
                tomorrowExisting.add(updated)
                movedCount++
            }
            postMessage("Moved $movedCount selected task(s) to Tomorrow ($tomorrow)!")
        }
    }

    // --- Settings Actions ---
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(language = language) }
            postMessage("Language changed to ${language.displayName}")
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(darkTheme = enabled) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(notificationsEnabled = enabled) }
            val tasks = repository.getAllTasksSnapshot()
            if (enabled) {
                tasks.filter { it.hasReminder && !it.isCompleted }.forEach {
                    TaskReminderScheduler.scheduleTaskReminder(appContext, it)
                }
                postMessage("Reminders enabled!")
            } else {
                tasks.forEach { TaskReminderScheduler.cancelTaskReminder(appContext, it.id) }
                postMessage("Reminders paused.")
            }
        }
    }

    fun updatePlanningHours(
        defaultStart: String,
        workStart: String,
        workEnd: String,
        reminderLeadMinutes: Int
    ) {
        if (!TimeUtils.isValidHhMm(defaultStart) || !TimeUtils.isValidHhMm(workStart) || !TimeUtils.isValidHhMm(workEnd)) {
            postMessage("Please enter valid times in HH:MM format (e.g., 07:00).")
            return
        }
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(
                    defaultPlanningStartTime = defaultStart.trim(),
                    workingHoursStart = workStart.trim(),
                    workingHoursEnd = workEnd.trim(),
                    reminderLeadMinutes = reminderLeadMinutes.coerceIn(0, 120)
                )
            }
            postMessage("Saved planning & reminder preferences!")
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            val all = repository.getAllTasksSnapshot()
            all.forEach { TaskReminderScheduler.cancelTaskReminder(appContext, it.id) }
            repository.clearAllLocalData()
            localState.update {
                it.copy(
                    aiProposal = null,
                    dismissedRescheduleTaskIds = emptySet(),
                    snackbarMessage = "All local task data has been permanently cleared."
                )
            }
        }
    }

    class Factory(
        private val repository: NovaRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NovaViewModel(repository, appContext) as T
        }
    }
}
