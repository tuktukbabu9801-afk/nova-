package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.LocalizedCopy
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmeraldSuccess

@Composable
fun ProgressDashboardScreen(
    uiState: NovaUiState,
    copy: LocalizedCopy,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("progress_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = copy.navProgress,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "All metrics are calculated strictly from your saved task records.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Daily Completion Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_daily_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Completion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.todayCompleted} of ${uiState.todayTotal} tasks completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${uiState.todayCompletionPercent}%",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("progress_daily_percent_value")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (uiState.todayCompletionPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = NovaEmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (uiState.todayTotal == 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "0% — No tasks scheduled for today yet. Add a task to start tracking today's progress!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("progress_empty_today_note")
                        )
                    }
                }
            }
        }

        // 2. Activity Streak & Most Productive Day Row (Never invent streaks without history)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("progress_streak_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = NovaAmberWarning
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Activity Streak",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.hasStreakHistory && uiState.currentStreakDays > 0) {
                            Text(
                                text = "${uiState.currentStreakDays} Day(s)",
                                style = MaterialTheme.typography.headlineSmall,
                                color = NovaAmberWarning,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Consistent daily completions!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No streak yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Complete your first personal task to start a real activity streak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("progress_productive_day_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = NovaElectricBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Top Day",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.mostProductiveDayLabel != null) {
                            Text(
                                text = uiState.mostProductiveDayLabel,
                                style = MaterialTheme.typography.headlineSmall,
                                color = NovaElectricBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Most completed tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Needs data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Complete at least 2 tasks to unlock your most productive day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Weekly Task Completion Bar Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_weekly_chart_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Last 7 Days Completion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        uiState.weeklyStats.forEach { stat ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text(
                                    text = "${stat.completedTasks}/${stat.totalTasks}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val fillFraction = (stat.percentage / 100f).coerceIn(0.06f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(if (stat.totalTasks == 0) 0.05f else fillFraction)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (stat.completedTasks > 0) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline
                                                }
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stat.dayShortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: NovaUiState,
    copy: LocalizedCopy,
    onLanguageChange: (AppLanguage) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onSavePlanningHours: (String, String, String, Int) -> Unit,
    onLoadSampleTasks: () -> Unit,
    onRemoveSampleTasks: () -> Unit,
    onClearAllLocalData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var defaultPlanStart by remember(uiState.settings.defaultPlanningStartTime) {
        mutableStateOf(uiState.settings.defaultPlanningStartTime)
    }
    var workStart by remember(uiState.settings.workingHoursStart) {
        mutableStateOf(uiState.settings.workingHoursStart)
    }
    var workEnd by remember(uiState.settings.workingHoursEnd) {
        mutableStateOf(uiState.settings.workingHoursEnd)
    }
    var reminderLead by remember(uiState.settings.reminderLeadMinutes) {
        mutableIntStateOf(uiState.settings.reminderLeadMinutes)
    }
    var showClearDataDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = copy.navSettings,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Customize language, appearance, working hours, and local storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Language Preference (Actually switches interface copy across Hinglish, English, Hindi)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Language Preference (भाषा)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = uiState.settings.language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text(lang.displayName) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Theme & Notifications
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dark Mode Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Deep charcoal & electric violet (Default)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = uiState.settings.darkTheme,
                            onCheckedChange = onDarkThemeChange,
                            modifier = Modifier.testTag("settings_dark_theme_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = NovaAmberWarning)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Task Reminders & Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Request Android notification permission when enabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = uiState.settings.notificationsEnabled,
                            onCheckedChange = onNotificationsChange,
                            modifier = Modifier.testTag("settings_notifications_switch")
                        )
                    }
                }
            }
        }

        // 3. Default Daily Planning Start Time, Working Hours & Reminder Lead Time
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = NovaElectricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Planning & Reminder Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = defaultPlanStart,
                        onValueChange = { defaultPlanStart = it },
                        label = { Text("Default Daily Planning Start Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_plan_start_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = workStart,
                            onValueChange = { workStart = it },
                            label = { Text("Work Start (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = workEnd,
                            onValueChange = { workEnd = it },
                            label = { Text("Work End (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Default Reminder Lead Time", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = reminderLead == mins,
                                onClick = { reminderLead = mins },
                                label = { Text("${mins}m before") }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSavePlanningHours(defaultPlanStart, workStart, workEnd, reminderLead)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_hours_btn")
                    ) {
                        Text("Save Schedule Preferences")
                    }
                }
            }
        }

        // 4. Demo Data & Clear Local Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Demo & Local Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onLoadSampleTasks,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_load_demo_btn")
                        ) {
                            Text("Load Sample Tasks")
                        }
                        if (uiState.hasSampleTasks) {
                            OutlinedButton(
                                onClick = onRemoveSampleTasks,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_remove_demo_btn")
                            ) {
                                Text("Remove Samples")
                            }
                        }
                    }

                    Button(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_data_btn")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Local Data")
                    }
                }
            }
        }

        // 5. About NOVA & Privacy / Security Disclosure
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_nova_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = NovaEmeraldSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About NOVA — AI Life Manager (v1.0)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "\"Plan Your Day. Take Control of Your Life.\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Privacy & Local Storage: All tasks, schedules, and progress statistics are stored locally on your Android device using SQLite (Room) and Jetpack DataStore. No login or personal account is required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• AI Processing Disclosure: By default, NOVA uses its On-Device Smart Rule-Based Scheduling Engine, so zero task data is sent to external servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Local Data?") },
            text = {
                Text("Warning: This will permanently delete all your saved tasks, schedules, and progress history from this device. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataDialog = false
                        onClearAllLocalData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_data_btn")
                ) {
                    Text("Yes, Delete All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
