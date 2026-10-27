package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LocalizedCopy
import com.example.data.NovaTask
import com.example.data.TaskCategory
import com.example.data.TaskPriority
import com.example.data.TimeUtils
import com.example.planner.ProposedTaskItem
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmeraldSuccess

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPlannerAndNightScreen(
    uiState: NovaUiState,
    copy: LocalizedCopy,
    initialNightMode: Boolean = false,
    onPromptChange: (String) -> Unit,
    onGeneratePlan: (String?) -> Unit,
    onRegeneratePlan: () -> Unit,
    onUpdateProposedItem: (ProposedTaskItem) -> Unit,
    onRemoveProposedItem: (Int) -> Unit,
    onCancelPlan: () -> Unit,
    onConfirmAndSavePlan: () -> Unit,
    onMoveSelectedTasksToTomorrow: (Set<Long>) -> Unit,
    onQuickAddTomorrowTask: (String, String, TaskPriority, TaskCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var isNightTabSelected by remember(initialNightMode) { mutableStateOf(initialNightMode) }
    var showConfirmSaveDialog by remember { mutableStateOf(false) }
    var editingProposedItem by remember { mutableStateOf<ProposedTaskItem?>(null) }
    var showAiProviderInfoDialog by remember { mutableStateOf(false) }

    // Night Planning state: which unfinished today tasks the user checked to move to tomorrow
    var selectedUnfinishedIds by remember { mutableStateOf(setOf<Long>()) }
    var tomorrowQuickTitle by remember { mutableStateOf("") }
    var tomorrowQuickTime by remember { mutableStateOf("09:00") }
    var tomorrowQuickPriority by remember { mutableStateOf(TaskPriority.HIGH) }
    var tomorrowQuickCategory by remember { mutableStateOf(TaskCategory.STUDY) }

    val unfinishedToday = remember(uiState.todayTasks) {
        uiState.todayTasks.filter { !it.isCompleted }
    }
    val tomorrowIso = remember { TimeUtils.tomorrowIso() }
    val tomorrowTasks = remember(uiState.allTasks, tomorrowIso) {
        uiState.allTasks.filter { it.dateIso == tomorrowIso }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_planner_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Switcher: AI Daily Planner vs Night Planning ("Plan Tomorrow")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !isNightTabSelected,
                    onClick = { isNightTabSelected = false },
                    label = { Text("AI Daily Planner") },
                    leadingIcon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("tab_ai_daily_planner")
                )
                FilterChip(
                    selected = isNightTabSelected,
                    onClick = { isNightTabSelected = true },
                    label = { Text("Plan Tomorrow (Night Prep)") },
                    leadingIcon = {
                        Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("tab_night_planning")
                )
            }
        }

        if (!isNightTabSelected) {
            // --- AI DAILY PLANNER SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = copy.aiPlannerHeader,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            IconButton(
                                onClick = { showAiProviderInfoDialog = true },
                                modifier = Modifier.testTag("ai_engine_info_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "How AI & Rule-Based Scheduling Works",
                                    tint = NovaElectricBlue
                                )
                            }
                        }

                        // Honest Disclosure Badge: Rule-Based vs Cloud AI
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(10.dp)
                                .testTag("ai_honest_disclosure_banner")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NovaElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${copy.aiModeDisclosure} Tap (i) to see how cloud LLM APIs can be connected later.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = copy.aiPlannerSubheader,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.aiPromptText,
                            onValueChange = onPromptChange,
                            label = { Text("Describe your tasks in Hinglish, Hindi, or English") },
                            placeholder = {
                                Text("Kal subah 7 baje uthna hai, 9 se 11 baje tak padhai karni hai, dopahar ko business par kaam karna hai aur shaam ko exercise karni hai.")
                            },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_planner_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Try an example prompt:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val hinglishSample = "Kal subah 7 baje uthna hai, 9 se 11 baje tak padhai karni hai, dopahar ko business par kaam karna hai aur shaam ko exercise karni hai."
                            val englishSample = "Today study from 10am to 12pm, work on client project at 3pm for 90 mins, and evening workout at 6pm."
                            SuggestionChip(
                                onClick = {
                                    onPromptChange(hinglishSample)
                                    onGeneratePlan(hinglishSample)
                                },
                                label = { Text("Hinglish Full Day Example") },
                                modifier = Modifier.testTag("sample_prompt_hinglish_chip")
                            )
                            SuggestionChip(
                                onClick = {
                                    onPromptChange(englishSample)
                                    onGeneratePlan(englishSample)
                                },
                                label = { Text("English Study & Work Example") },
                                modifier = Modifier.testTag("sample_prompt_english_chip")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onGeneratePlan(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_schedule_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Structured Schedule")
                        }
                    }
                }
            }

            // Proposed Schedule Review Card
            val proposal = uiState.aiProposal
            if (proposal != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.large
                            )
                            .testTag("ai_proposal_review_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Proposed Schedule for ${TimeUtils.formatReadableDate(proposal.targetDateIso)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Generated by: ${proposal.engineLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Clarifying Questions (if any critical info was omitted)
                            if (proposal.clarifyingQuestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.HelpOutline,
                                                contentDescription = null,
                                                tint = NovaAmberWarning,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Clarifying Questions",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = NovaAmberWarning
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        proposal.clarifyingQuestions.forEach { q ->
                                            Text("• $q", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            // Assumptions Explained Briefly
                            if (proposal.assumptions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Planner Assumptions & Notes:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                proposal.assumptions.forEach { note ->
                                    Text(
                                        text = "• $note",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            proposal.proposedTasks.forEach { item ->
                                ProposedTaskRow(
                                    item = item,
                                    onEdit = { editingProposedItem = item },
                                    onRemove = { onRemoveProposedItem(item.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // 4 Required Actions: Accept Schedule, Edit Schedule, Regenerate Schedule, Cancel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showConfirmSaveDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("accept_schedule_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept Schedule")
                                }
                                OutlinedButton(
                                    onClick = {
                                        proposal.proposedTasks.firstOrNull()?.let { editingProposedItem = it }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_schedule_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Schedule")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onRegeneratePlan,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("regenerate_schedule_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Regenerate")
                                }
                                TextButton(
                                    onClick = onCancelPlan,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_schedule_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- NIGHT PLANNING ("PLAN TOMORROW") SECTION ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("night_planning_header_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = copy.nightPlanTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${copy.nightPlanQuote}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaElectricBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Review unfinished tasks from today and choose which ones to move to tomorrow ($tomorrowIso). NOVA never copies unfinished tasks automatically without your selection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Step 1: Review Unfinished Tasks from Today
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. Unfinished Tasks From Today (${unfinishedToday.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (unfinishedToday.isEmpty()) {
                            Text(
                                text = "All of today's tasks are completed or none are pending!",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaEmeraldSuccess
                            )
                        } else {
                            unfinishedToday.forEach { task ->
                                val isChecked = task.id in selectedUnfinishedIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedUnfinishedIds = if (checked) {
                                                selectedUnfinishedIds + task.id
                                            } else {
                                                selectedUnfinishedIds - task.id
                                            }
                                        },
                                        modifier = Modifier.testTag("night_move_checkbox_${task.id}")
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${task.formattedTimeRange()} • ${task.priority.labelEn}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onMoveSelectedTasksToTomorrow(selectedUnfinishedIds)
                                    selectedUnfinishedIds = emptySet()
                                },
                                enabled = selectedUnfinishedIds.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("move_selected_to_tomorrow_btn")
                            ) {
                                Text("Move ${selectedUnfinishedIds.size} Selected Task(s) to Tomorrow")
                            }
                        }
                    }
                }
            }

            // Step 2: Add Tomorrow's Priority Task
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "2. Add Tomorrow's Tasks ($tomorrowIso)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = tomorrowQuickTitle,
                            onValueChange = { tomorrowQuickTitle = it },
                            label = { Text("Tomorrow's Task Title") },
                            placeholder = { Text("e.g., Morning Revision or Client Pitch") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tomorrow_quick_title_input")
                        )
                        OutlinedTextField(
                            value = tomorrowQuickTime,
                            onValueChange = { tomorrowQuickTime = it },
                            label = { Text("Start Time (HH:MM 24h)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tomorrow_quick_time_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TaskPriority.entries.forEach { p ->
                                FilterChip(
                                    selected = tomorrowQuickPriority == p,
                                    onClick = { tomorrowQuickPriority = p },
                                    label = { Text(p.labelEn) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (tomorrowQuickTitle.isNotBlank()) {
                                    onQuickAddTomorrowTask(
                                        tomorrowQuickTitle,
                                        tomorrowQuickTime,
                                        tomorrowQuickPriority,
                                        tomorrowQuickCategory
                                    )
                                    tomorrowQuickTitle = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_tomorrow_task_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Tomorrow's Plan")
                        }
                    }
                }
            }

            // Step 3: Tomorrow's Current Schedule Preview
            item {
                Text(
                    text = "Tomorrow's Confirmed Schedule (${tomorrowTasks.size} Tasks)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (tomorrowTasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks scheduled for tomorrow yet. Add tasks above or use the AI Daily Planner with 'Kal...'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(tomorrowTasks, key = { it.id }) { tTask ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${tTask.category.emoji} ${tTask.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${tTask.formattedTimeRange()} • ${tTask.priority.labelEn}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog Before Saving AI-Generated Schedule (Never overwrite existing tasks silently)
    val currentProposal = uiState.aiProposal
    if (showConfirmSaveDialog && currentProposal != null) {
        AlertDialog(
            onDismissRequest = { showConfirmSaveDialog = false },
            title = { Text("Confirm & Save AI Schedule?") },
            text = {
                Text(
                    "This will add ${currentProposal.proposedTasks.size} scheduled tasks to ${currentProposal.targetDateIso}. Your existing tasks will NOT be overwritten or deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSaveDialog = false
                        onConfirmAndSavePlan()
                    },
                    modifier = Modifier.testTag("confirm_save_ai_schedule_btn")
                ) {
                    Text("Confirm & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSaveDialog = false }) {
                    Text("Back to Review")
                }
            }
        )
    }

    // Edit Proposed Task Item Dialog
    if (editingProposedItem != null) {
        val item = editingProposedItem!!
        var editTitle by remember(item) { mutableStateOf(item.title) }
        var editTime by remember(item) { mutableStateOf(item.suggestedStartTime) }
        var editDuration by remember(item) { mutableStateOf(item.estimatedDurationMinutes.toString()) }
        var editPriority by remember(item) { mutableStateOf(item.priority) }

        AlertDialog(
            onDismissRequest = { editingProposedItem = null },
            title = { Text("Edit Proposed Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Task Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Start Time (HH:MM 24h)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Duration (Minutes)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.entries.forEach { p ->
                            FilterChip(
                                selected = editPriority == p,
                                onClick = { editPriority = p },
                                label = { Text(p.labelEn) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && TimeUtils.isValidHhMm(editTime)) {
                            onUpdateProposedItem(
                                item.copy(
                                    title = editTitle.trim(),
                                    suggestedStartTime = editTime.trim(),
                                    estimatedDurationMinutes = (editDuration.toIntOrNull() ?: 60).coerceIn(10, 360),
                                    priority = editPriority
                                )
                            )
                            editingProposedItem = null
                        }
                    }
                ) {
                    Text("Update Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProposedItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Honest AI Architecture & Cloud Provider Connection Info Dialog
    if (showAiProviderInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAiProviderInfoDialog = false },
            title = { Text("About NOVA's Scheduling Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Current Mode: On-Device Smart Rule-Based Engine",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "NOVA currently parses English, Hinglish, and Hindi instructions locally on your smartphone at zero cost, with 100% offline privacy. No task data is transmitted to external servers.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Connecting an External Cloud AI Provider Later",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To upgrade to cloud LLM inference (e.g., Gemini API via Firebase AI Logic or server-side API keys), provision Firebase AI Logic in AI Studio or configure GEMINI_API_KEY in the AI Studio Secrets panel. Note: When using external cloud AI, your prompt text is sent to the AI provider to generate schedules.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiProviderInfoDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun ProposedTaskRow(
    item: ProposedTaskItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("proposed_task_row_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isBreak) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${TimeUtils.format12Hour(item.suggestedStartTime)} (${item.estimatedDurationMinutes}m) • ${item.category.emoji} ${item.category.labelEn}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NovaElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Priority: ${item.priority.labelEn}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.conflictNote != null) {
                        Text(
                            text = item.conflictNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = NovaAmberWarning
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit proposed task", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove proposed task",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
