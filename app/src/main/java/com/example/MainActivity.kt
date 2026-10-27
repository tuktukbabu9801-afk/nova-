package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.NovaDatabase
import com.example.data.NovaRepository
import com.example.data.NovaStrings
import com.example.data.NovaTask
import com.example.data.RecurringType
import com.example.data.TimeUtils
import com.example.notifications.TaskReminderScheduler
import com.example.ui.AiPlannerAndNightScreen
import com.example.ui.DeleteTaskConfirmDialog
import com.example.ui.HomeDashboardScreen
import com.example.ui.MyTasksScreen
import com.example.ui.NovaViewModel
import com.example.ui.ProgressDashboardScreen
import com.example.ui.RescheduleTaskDialog
import com.example.ui.SettingsScreen
import com.example.ui.TaskEditorDialog
import com.example.ui.theme.MyApplicationTheme

enum class NovaSection(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", Icons.Filled.Home, Icons.Outlined.Home),
    TASKS("tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    PLANNER("planner", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    PROGRESS("progress", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TaskReminderScheduler.ensureChannel(this)

        val db = NovaDatabase.getInstance(applicationContext)
        val repository = NovaRepository(db.taskDao(), applicationContext)

        setContent {
            val novaViewModel: NovaViewModel = viewModel(
                factory = NovaViewModel.Factory(repository, applicationContext)
            )
            NovaAppRoot(viewModel = novaViewModel)
        }
    }
}

@Composable
fun NovaAppRoot(viewModel: NovaViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copy = remember(uiState.settings.language) {
        NovaStrings.forLanguage(uiState.settings.language)
    }

    var currentSection by remember { mutableStateOf(NovaSection.HOME) }
    var openPlannerInNightMode by remember { mutableStateOf(false) }

    // Dialog states
    var showTaskEditor by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<NovaTask?>(null) }
    var taskPendingDelete by remember { mutableStateOf<NovaTask?>(null) }
    var rescheduleModalState by remember { mutableStateOf<Pair<NovaTask, String>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Runtime POST_NOTIFICATIONS launcher (only requested when user explicitly enables reminders)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
    }

    val requestEnableReminders: (Boolean) -> Unit = { wantEnabled ->
        if (wantEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !TaskReminderScheduler.hasNotificationPermission(context)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setNotificationsEnabled(true)
            }
        } else {
            viewModel.setNotificationsEnabled(false)
        }
    }

    // BackHandler on secondary sections to return to Home cleanly
    BackHandler(enabled = currentSection != NovaSection.HOME) {
        currentSection = NovaSection.HOME
    }

    MyApplicationTheme(darkTheme = uiState.settings.darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    if (currentSection == NovaSection.HOME || currentSection == NovaSection.TASKS) {
                        FloatingActionButton(
                            onClick = {
                                taskBeingEdited = null
                                showTaskEditor = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("fab_add_task")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add new task"
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                            NovaSection.entries.forEach { section ->
                                val label = when (section) {
                                    NovaSection.HOME -> copy.navHome
                                    NovaSection.TASKS -> copy.navTasks
                                    NovaSection.PLANNER -> copy.navPlanner
                                    NovaSection.PROGRESS -> copy.navProgress
                                    NovaSection.SETTINGS -> copy.navSettings
                                }
                                val isSelected = currentSection == section
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (section != NovaSection.PLANNER) {
                                            openPlannerInNightMode = false
                                        }
                                        currentSection = section
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) section.selectedIcon else section.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_item_${section.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(modifier = Modifier.testTag("navigation_rail")) {
                            NovaSection.entries.forEach { section ->
                                val label = when (section) {
                                    NovaSection.HOME -> copy.navHome
                                    NovaSection.TASKS -> copy.navTasks
                                    NovaSection.PLANNER -> copy.navPlanner
                                    NovaSection.PROGRESS -> copy.navProgress
                                    NovaSection.SETTINGS -> copy.navSettings
                                }
                                val isSelected = currentSection == section
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (section != NovaSection.PLANNER) {
                                            openPlannerInNightMode = false
                                        }
                                        currentSection = section
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) section.selectedIcon else section.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("nav_item_${section.route}")
                                )
                            }
                        }
                    }

                    when (currentSection) {
                        NovaSection.HOME -> HomeDashboardScreen(
                            uiState = uiState,
                            copy = copy,
                            onAddTaskClick = {
                                taskBeingEdited = null
                                showTaskEditor = true
                            },
                            onPlanWithAiClick = {
                                openPlannerInNightMode = false
                                currentSection = NovaSection.PLANNER
                            },
                            onPlanTomorrowClick = {
                                openPlannerInNightMode = true
                                currentSection = NovaSection.PLANNER
                            },
                            onToggleTaskComplete = viewModel::toggleTaskCompletion,
                            onEditTask = { task ->
                                taskBeingEdited = task
                                showTaskEditor = true
                            },
                            onDeleteTaskRequest = { task ->
                                taskPendingDelete = task
                            },
                            onRescheduleRequest = { task, suggestedSlot ->
                                rescheduleModalState = task to suggestedSlot
                            },
                            onKeepTaskPending = viewModel::keepTaskPending,
                            onLoadSampleTasks = viewModel::loadSampleTasks,
                            onRemoveSampleTasks = viewModel::removeSampleTasks,
                            modifier = Modifier.weight(1f)
                        )

                        NovaSection.TASKS -> MyTasksScreen(
                            uiState = uiState,
                            copy = copy,
                            onAddTaskClick = {
                                taskBeingEdited = null
                                showTaskEditor = true
                            },
                            onFilterChange = viewModel::setTaskFilter,
                            onCategoryFilterChange = viewModel::setCategoryFilter,
                            onToggleComplete = viewModel::toggleTaskCompletion,
                            onEditTask = { task ->
                                taskBeingEdited = task
                                showTaskEditor = true
                            },
                            onDeleteTaskRequest = { task ->
                                taskPendingDelete = task
                            },
                            onRescheduleRequest = { task, suggestedSlot ->
                                rescheduleModalState = task to suggestedSlot
                            },
                            onSnoozeReminder = viewModel::snoozeTaskReminder,
                            onKeepPending = viewModel::keepTaskPending,
                            onRequestNotificationPermission = { requestEnableReminders(true) },
                            modifier = Modifier.weight(1f)
                        )

                        NovaSection.PLANNER -> AiPlannerAndNightScreen(
                            uiState = uiState,
                            copy = copy,
                            initialNightMode = openPlannerInNightMode,
                            onPromptChange = viewModel::updateAiPromptText,
                            onGeneratePlan = viewModel::generateAiPlan,
                            onRegeneratePlan = viewModel::regenerateAiPlan,
                            onUpdateProposedItem = viewModel::updateProposedTaskItem,
                            onRemoveProposedItem = viewModel::removeProposedTaskItem,
                            onCancelPlan = viewModel::cancelAiPlan,
                            onConfirmAndSavePlan = viewModel::confirmAndSaveAiPlan,
                            onMoveSelectedTasksToTomorrow = viewModel::moveSelectedTasksToTomorrow,
                            onQuickAddTomorrowTask = { title, time, priority, category ->
                                viewModel.saveTask(
                                    existingId = 0L,
                                    title = title,
                                    description = "Planned via Night Prep for tomorrow",
                                    dateIso = TimeUtils.tomorrowIso(),
                                    startTime = time,
                                    durationMinutes = 60,
                                    priority = priority,
                                    category = category,
                                    hasReminder = true,
                                    reminderTime = "",
                                    recurring = RecurringType.NONE
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        NovaSection.PROGRESS -> ProgressDashboardScreen(
                            uiState = uiState,
                            copy = copy,
                            modifier = Modifier.weight(1f)
                        )

                        NovaSection.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            copy = copy,
                            onLanguageChange = viewModel::setLanguage,
                            onDarkThemeChange = viewModel::setDarkTheme,
                            onNotificationsChange = requestEnableReminders,
                            onSavePlanningHours = viewModel::updatePlanningHours,
                            onLoadSampleTasks = viewModel::loadSampleTasks,
                            onRemoveSampleTasks = viewModel::removeSampleTasks,
                            onClearAllLocalData = viewModel::clearAllLocalData,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Create / Edit Task Modal
        if (showTaskEditor) {
            TaskEditorDialog(
                initialTask = taskBeingEdited,
                onDismiss = {
                    showTaskEditor = false
                    taskBeingEdited = null
                },
                onSave = viewModel::saveTask
            )
        }

        // Delete Confirmation Modal
        val deletingTask = taskPendingDelete
        if (deletingTask != null) {
            DeleteTaskConfirmDialog(
                task = deletingTask,
                onDismiss = { taskPendingDelete = null },
                onConfirmDelete = {
                    viewModel.deleteTask(deletingTask)
                    taskPendingDelete = null
                }
            )
        }

        // Smart Rescheduling Modal
        val reschedulingPair = rescheduleModalState
        if (reschedulingPair != null) {
            RescheduleTaskDialog(
                task = reschedulingPair.first,
                suggestedTime = reschedulingPair.second,
                allTasks = uiState.allTasks,
                onDismiss = { rescheduleModalState = null },
                onConfirmReschedule = { newDate, newTime ->
                    viewModel.rescheduleTask(reschedulingPair.first, newDate, newTime)
                },
                onMarkComplete = {
                    viewModel.toggleTaskCompletion(reschedulingPair.first)
                },
                onKeepPending = {
                    viewModel.keepTaskPending(reschedulingPair.first.id)
                }
            )
        }
    }
}
