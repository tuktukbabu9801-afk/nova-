package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LocalizedCopy
import com.example.data.NovaTask
import com.example.data.TaskCategory
import com.example.data.TaskFilter
import com.example.data.TimeUtils
import com.example.notifications.TaskReminderScheduler
import com.example.planner.RuleBasedAiPlanner
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaElectricBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyTasksScreen(
    uiState: NovaUiState,
    copy: LocalizedCopy,
    onAddTaskClick: () -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onCategoryFilterChange: (TaskCategory?) -> Unit,
    onToggleComplete: (NovaTask) -> Unit,
    onEditTask: (NovaTask) -> Unit,
    onDeleteTaskRequest: (NovaTask) -> Unit,
    onRescheduleRequest: (NovaTask, String) -> Unit,
    onSnoozeReminder: (NovaTask) -> Unit,
    onKeepPending: (Long) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasNotifPerm = remember(uiState.settings.notificationsEnabled) {
        TaskReminderScheduler.hasNotificationPermission(context)
    }
    val canExactAlarms = remember { TaskReminderScheduler.canScheduleExactAlarms(context) }
    var showReminderCenter by remember { mutableStateOf(true) }

    val todayIso = TimeUtils.todayIso()
    val nowMins = TimeUtils.currentMinutesOfDay()

    val filteredTasks = remember(
        uiState.allTasks,
        uiState.selectedFilter,
        uiState.selectedCategoryFilter,
        todayIso,
        nowMins
    ) {
        val byStatus = when (uiState.selectedFilter) {
            TaskFilter.ALL -> uiState.allTasks
            TaskFilter.TODAY -> uiState.todayTasks
            TaskFilter.PENDING -> uiState.allTasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> uiState.allTasks.filter { it.isCompleted }
            TaskFilter.OVERDUE -> uiState.overdueTasks
            TaskFilter.UPCOMING -> uiState.upcomingTasks
        }
        val cat = uiState.selectedCategoryFilter
        if (cat == null) byStatus else byStatus.filter { it.category == cat }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_tasks_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = copy.navTasks,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${uiState.allTasks.count { !it.isCompleted }} pending • ${uiState.overdueTasks.size} overdue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddTaskClick,
                    modifier = Modifier.testTag("tasks_screen_add_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Task")
                }
            }
        }

        // Status Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filterItems = listOf(
                    TaskFilter.ALL to "All (${uiState.allTasks.size})",
                    TaskFilter.TODAY to "Today (${uiState.todayTasks.size})",
                    TaskFilter.PENDING to "Pending (${uiState.allTasks.count { !it.isCompleted }})",
                    TaskFilter.COMPLETED to "Completed (${uiState.allTasks.count { it.isCompleted }})",
                    TaskFilter.OVERDUE to "Overdue (${uiState.overdueTasks.size})",
                    TaskFilter.UPCOMING to "Upcoming (${uiState.upcomingTasks.size})"
                )
                items(filterItems) { (filter, label) ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Category Filter Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == null,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("All Categories") }
                    )
                }
                items(TaskCategory.entries.toList()) { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == category,
                        onClick = {
                            onCategoryFilterChange(
                                if (uiState.selectedCategoryFilter == category) null else category
                            )
                        },
                        label = { Text("${category.emoji} ${category.labelEn}") }
                    )
                }
            }
        }

        // Reminder Center Card (Upcoming Reminders + Snooze / Reschedule / Complete Now / Keep Pending + Technical Disclosure)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_center_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = NovaAmberWarning
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder Center (${uiState.activeReminderTasks.size} Active)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { showReminderCenter = !showReminderCenter }) {
                            Text(if (showReminderCenter) "Hide" else "Expand")
                        }
                    }

                    if (showReminderCenter) {
                        Spacer(modifier = Modifier.height(6.dp))
                        // Permission & Android Delivery Disclosure
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NovaElectricBlue,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val exactStatus = if (canExactAlarms) "Exact Alarms Available" else "Inexact Window Fallback"
                            Text(
                                text = "Android AlarmManager ($exactStatus): Delivery when the app is closed depends on Android Notification Permission and device battery optimization settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!uiState.settings.notificationsEnabled || !hasNotifPerm) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enable_reminders_permission_btn")
                            ) {
                                Icon(Icons.Default.AlarmOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enable Android Notification Reminders")
                            }
                        }

                        if (uiState.activeReminderTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            uiState.activeReminderTasks.take(3).forEach { remTask ->
                                val effectiveRemTime = remTask.snoozedUntilTime?.takeIf { it.isNotBlank() }
                                    ?: remTask.reminderTime.ifBlank { remTask.startTime }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${remTask.title} • Reminder at ${TimeUtils.format12Hour(effectiveRemTime)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilledTonalButton(
                                                onClick = { onSnoozeReminder(remTask) },
                                                modifier = Modifier.testTag("snooze_reminder_btn_${remTask.id}")
                                            ) {
                                                Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Snooze +15m")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val slot = RuleBasedAiPlanner.suggestRescheduleSlot(
                                                        remTask,
                                                        uiState.todayTasks
                                                    )
                                                    onRescheduleRequest(remTask, slot)
                                                }
                                            ) {
                                                Text("Reschedule")
                                            }
                                            Button(onClick = { onToggleComplete(remTask) }) {
                                                Text("Complete Now")
                                            }
                                            TextButton(onClick = { onKeepPending(remTask.id) }) {
                                                Text("Keep Pending")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filtered Task List or Empty State
        if (filteredTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks_empty_filter_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No tasks match this filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Switch filters above or tap 'New Task' to add one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val groupedByDate = filteredTasks.groupBy { it.dateIso }
            groupedByDate.forEach { (dateIso, tasksForDate) ->
                item(key = "header_$dateIso") {
                    Text(
                        text = TimeUtils.formatReadableDate(dateIso, todayIso),
                        style = MaterialTheme.typography.titleSmall,
                        color = NovaElectricBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(tasksForDate, key = { it.id }) { task ->
                    NovaTaskCard(
                        task = task,
                        onToggleComplete = { onToggleComplete(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTaskRequest(task) },
                        onReschedule = {
                            val slot = RuleBasedAiPlanner.suggestRescheduleSlot(task, tasksForDate)
                            onRescheduleRequest(task, slot)
                        }
                    )
                }
            }
        }
    }
}
