package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LocalizedCopy
import com.example.data.NovaTask
import com.example.data.RecurringType
import com.example.data.TaskCategory
import com.example.data.TaskPriority
import com.example.data.TimeUtils
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaCoralHigh
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmeraldSuccess

@Composable
fun HomeDashboardScreen(
    uiState: NovaUiState,
    copy: LocalizedCopy,
    onAddTaskClick: () -> Unit,
    onPlanWithAiClick: () -> Unit,
    onPlanTomorrowClick: () -> Unit,
    onToggleTaskComplete: (NovaTask) -> Unit,
    onEditTask: (NovaTask) -> Unit,
    onDeleteTaskRequest: (NovaTask) -> Unit,
    onRescheduleRequest: (NovaTask, String) -> Unit,
    onKeepTaskPending: (Long) -> Unit,
    onLoadSampleTasks: () -> Unit,
    onRemoveSampleTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayFormatted = remember { TimeUtils.formatReadableDate(TimeUtils.todayIso()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Header Card with NOVA Branding, Current Date, Personalized Greeting & Daily Motivation
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_hero_card"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nova_hero_banner),
                        contentDescription = "NOVA cosmic banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "NOVA Logo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NOVA — AI LIFE MANAGER",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NovaElectricBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = todayFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = copy.greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${copy.motivation}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Primary Action Buttons (+ Add New Task & Plan My Day with AI & Night Prep)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddTaskClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("home_add_task_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(copy.addNewTask, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    FilledTonalButton(
                        onClick = onPlanWithAiClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("home_plan_ai_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(copy.planWithAi, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OutlinedButton(
                    onClick = onPlanTomorrowClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_plan_tomorrow_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(copy.planTomorrowBtn)
                }
            }
        }

        // 3. Sample Tasks Banner (if sample tasks are active)
        if (uiState.hasSampleTasks) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sample_data_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = copy.demoBannerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onRemoveSampleTasks,
                            modifier = Modifier.testTag("remove_sample_tasks_button")
                        ) {
                            Text(copy.removeDemoBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Real-Time Dashboard Statistics Grid
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_stats_card"),
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
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Overview",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${uiState.todayCompletionPercent}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("home_completion_percent_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (uiState.todayCompletionPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricBox(
                            label = copy.todayTasksLabel,
                            value = uiState.todayTotal.toString(),
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "stat_today_total",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            label = copy.completedLabel,
                            value = uiState.todayCompleted.toString(),
                            accentColor = NovaEmeraldSuccess,
                            testTag = "stat_today_completed",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            label = copy.pendingLabel,
                            value = uiState.todayPending.toString(),
                            accentColor = NovaAmberWarning,
                            testTag = "stat_today_pending",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Smart Rescheduling Assistant Card (when a pending task can be rescheduled)
        val rescheduleCandidate = uiState.suggestedRescheduleTask
        if (rescheduleCandidate != null) {
            item {
                val suggested12h = TimeUtils.format12Hour(uiState.suggestedRescheduleSlot)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            MaterialTheme.shapes.medium
                        )
                        .testTag("smart_reschedule_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = "Smart Reschedule Assistant",
                                tint = NovaElectricBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOVA Smart Rescheduling Assistant",
                                style = MaterialTheme.typography.titleSmall,
                                color = NovaElectricBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${rescheduleCandidate.title}\" (${rescheduleCandidate.formattedStartTime()}) — ${copy.reschedulePromptPrefix} $suggested12h?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onRescheduleRequest(rescheduleCandidate, uiState.suggestedRescheduleSlot)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("smart_reschedule_action_btn")
                            ) {
                                Text("Reschedule")
                            }
                            FilledTonalButton(
                                onClick = { onToggleTaskComplete(rescheduleCandidate) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("smart_complete_action_btn")
                            ) {
                                Text("Mark Complete")
                            }
                            TextButton(
                                onClick = { onKeepTaskPending(rescheduleCandidate.id) },
                                modifier = Modifier.testTag("smart_keep_pending_btn")
                            ) {
                                Text("Keep Pending")
                            }
                        }
                    }
                }
            }
        }

        // 6. Next Upcoming Task Spotlight
        val nextTask = uiState.nextUpcomingTask
        if (nextTask != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("next_upcoming_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = copy.nextUpcomingTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${nextTask.category.emoji} ${nextTask.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${nextTask.formattedTimeRange()} • ${nextTask.priority.labelEn} Priority",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onToggleTaskComplete(nextTask) },
                                modifier = Modifier.testTag("next_upcoming_complete_btn")
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }

        // 7. Chronological Timeline Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = copy.timelineTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                if (!uiState.hasSampleTasks && uiState.allTasks.isEmpty()) {
                    TextButton(
                        onClick = onLoadSampleTasks,
                        modifier = Modifier.testTag("load_sample_tasks_button")
                    ) {
                        Text(copy.loadDemoBtn)
                    }
                }
            }
        }

        // 8. Empty State OR Today's Chronological Task List
        if (uiState.todayTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_empty_state_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "No tasks scheduled",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = copy.emptyTasksTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = copy.emptyTasksSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onAddTaskClick,
                                modifier = Modifier.testTag("empty_state_add_task_btn")
                            ) {
                                Text(copy.addNewTask)
                            }
                            OutlinedButton(
                                onClick = onLoadSampleTasks,
                                modifier = Modifier.testTag("empty_state_load_demo_btn")
                            ) {
                                Text(copy.loadDemoBtn)
                            }
                        }
                    }
                }
            }
        } else {
            items(uiState.todayTasks, key = { it.id }) { task ->
                NovaTaskCard(
                    task = task,
                    onToggleComplete = { onToggleTaskComplete(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTaskRequest(task) },
                    onReschedule = {
                        val slot = RuleBasedAiPlannerSlotHelper.suggest(task, uiState.todayTasks)
                        onRescheduleRequest(task, slot)
                    }
                )
            }
        }
    }
}

private object RuleBasedAiPlannerSlotHelper {
    fun suggest(task: NovaTask, todayTasks: List<NovaTask>): String {
        return com.example.planner.RuleBasedAiPlanner.suggestRescheduleSlot(task, todayTasks)
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    value: String,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(vertical = 12.dp, horizontal = 10.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NovaTaskCard(
    task: NovaTask,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> NovaCoralHigh
        TaskPriority.MEDIUM -> NovaAmberWarning
        TaskPriority.LOW -> NovaEmeraldSuccess
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("task_toggle_complete_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = if (task.isCompleted) "Reopen task" else "Mark task completed",
                        tint = if (task.isCompleted) NovaEmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.formattedTimeRange(),
                            style = MaterialTheme.typography.labelMedium,
                            color = NovaElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                        if (task.isSample) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SAMPLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityColor.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = task.priority.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${task.category.emoji} ${task.category.labelEn}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (task.hasReminder) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Reminder active",
                                    tint = NovaAmberWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = TimeUtils.format12Hour(
                                        task.snoozedUntilTime?.takeIf { it.isNotBlank() }
                                            ?: task.reminderTime.ifBlank { task.startTime }
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NovaAmberWarning
                                )
                            }
                        }
                    }
                }

                Row {
                    if (!task.isCompleted) {
                        IconButton(
                            onClick = onReschedule,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("task_reschedule_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = "Reschedule task",
                                tint = NovaElectricBlue
                            )
                        }
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("task_edit_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("task_delete_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = NovaCoralHigh
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
    initialTask: NovaTask?,
    defaultDateIso: String = TimeUtils.todayIso(),
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
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
    ) -> String?
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var dateIso by remember { mutableStateOf(initialTask?.dateIso ?: defaultDateIso) }
    var startTime by remember { mutableStateOf(initialTask?.startTime ?: "09:00") }
    var durationStr by remember { mutableStateOf((initialTask?.durationMinutes ?: 60).toString()) }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var category by remember { mutableStateOf(initialTask?.category ?: TaskCategory.STUDY) }
    var hasReminder by remember { mutableStateOf(initialTask?.hasReminder ?: true) }
    var reminderTime by remember { mutableStateOf(initialTask?.reminderTime?.ifBlank { "08:50" } ?: "08:50") }
    var recurring by remember { mutableStateOf(initialTask?.recurring ?: RecurringType.NONE) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "Create New Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("task_editor_error_text")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g., Study Physics Chapter 4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Notes, links, or sub-steps") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = {
                            dateIso = it
                            validationError = null
                        },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("task_date_input")
                    )
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {
                            startTime = it
                            validationError = null
                        },
                        label = { Text("Start (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_time_input")
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { dateIso = TimeUtils.todayIso() }) {
                        Text("Set Today")
                    }
                    TextButton(onClick = { dateIso = TimeUtils.tomorrowIso() }) {
                        Text("Set Tomorrow")
                    }
                }

                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (Minutes)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_duration_input")
                )

                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.labelEn) },
                            modifier = Modifier.testTag("priority_chip_${p.name.lowercase()}")
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text("${c.emoji} ${c.labelEn}") },
                            modifier = Modifier.testTag("category_chip_${c.name.lowercase()}")
                        )
                    }
                }

                Text("Recurring Schedule", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurringType.entries.forEach { rec ->
                        FilterChip(
                            selected = recurring == rec,
                            onClick = { recurring = rec },
                            label = { Text(rec.labelEn) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Task Reminder", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it },
                        modifier = Modifier.testTag("task_reminder_switch")
                    )
                }

                if (hasReminder) {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Reminder Time (HH:MM, 24h)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_reminder_time_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationMins = durationStr.toIntOrNull() ?: 60
                    val error = onSave(
                        initialTask?.id ?: 0L,
                        title,
                        description,
                        dateIso,
                        startTime,
                        durationMins,
                        priority,
                        category,
                        hasReminder,
                        reminderTime,
                        recurring
                    )
                    if (error != null) {
                        validationError = error
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_task_confirm_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_task_editor_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RescheduleTaskDialog(
    task: NovaTask,
    suggestedTime: String,
    allTasks: List<NovaTask>,
    onDismiss: () -> Unit,
    onConfirmReschedule: (String, String) -> Unit,
    onMarkComplete: () -> Unit,
    onKeepPending: () -> Unit
) {
    var targetDateIso by remember { mutableStateOf(task.dateIso) }
    var targetTime by remember { mutableStateOf(suggestedTime) }

    val conflictingTask = remember(targetDateIso, targetTime, allTasks) {
        if (!TimeUtils.isValidHhMm(targetTime)) null
        else {
            val mins = TimeUtils.parseMinutesOfDay(targetTime)
            allTasks.firstOrNull {
                it.id != task.id && !it.isCompleted &&
                    it.overlapsWith(targetDateIso, mins, task.durationMinutes)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Rescheduling Assistant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your task \"${task.title}\" (${task.formattedStartTime()}) is still pending. Would you like to move it to ${TimeUtils.format12Hour(suggestedTime)} or choose a custom time?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetTime == suggestedTime && targetDateIso == task.dateIso,
                        onClick = {
                            targetDateIso = task.dateIso
                            targetTime = suggestedTime
                        },
                        label = { Text("Suggested: ${TimeUtils.format12Hour(suggestedTime)}") }
                    )
                    FilterChip(
                        selected = targetTime == "19:00",
                        onClick = { targetTime = "19:00" },
                        label = { Text("7:00 PM") }
                    )
                    FilterChip(
                        selected = targetDateIso == TimeUtils.tomorrowIso(),
                        onClick = { targetDateIso = TimeUtils.tomorrowIso() },
                        label = { Text("Tomorrow") }
                    )
                }

                OutlinedTextField(
                    value = targetDateIso,
                    onValueChange = { targetDateIso = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reschedule_date_input")
                )

                OutlinedTextField(
                    value = targetTime,
                    onValueChange = { targetTime = it },
                    label = { Text("New Start Time (HH:MM 24h)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reschedule_time_input")
                )

                if (conflictingTask != null) {
                    Text(
                        text = "Conflict Notice: Overlaps with '${conflictingTask.title}' (${conflictingTask.formattedTimeRange()}). Consider picking another slot.",
                        color = NovaAmberWarning,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("reschedule_conflict_warning")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmReschedule(targetDateIso, targetTime)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_reschedule_btn")
            ) {
                Text("Confirm Reschedule")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onMarkComplete()
                        onDismiss()
                    }
                ) {
                    Text("Mark Complete")
                }
                TextButton(
                    onClick = {
                        onKeepPending()
                        onDismiss()
                    }
                ) {
                    Text("Keep Pending")
                }
            }
        }
    )
}

@Composable
fun DeleteTaskConfirmDialog(
    task: NovaTask,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Task?") },
        text = {
            Text("Are you sure you want to permanently delete \"${task.title}\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_task_btn")
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_task_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
