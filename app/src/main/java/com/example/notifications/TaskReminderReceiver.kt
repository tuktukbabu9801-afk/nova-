package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NovaTask
import com.example.data.TimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "NOVA Task Reminder"
        val startTime = intent.getStringExtra(EXTRA_TASK_TIME) ?: ""
        val category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: ""

        TaskReminderScheduler.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = if (startTime.isNotBlank()) TimeUtils.format12Hour(startTime) else "Now"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NOVA • $title")
            .setContentText("Scheduled for $formattedTime ($category) — Tap to review, complete, or reschedule.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(taskId.toInt().coerceAtLeast(1), notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "nova_task_reminders"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_TIME = "extra_task_time"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
    }
}

object TaskReminderScheduler {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                TaskReminderReceiver.CHANNEL_ID,
                "NOVA Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivers scheduled daily task reminders from NOVA AI Life Manager"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleTaskReminder(context: Context, task: NovaTask): Boolean {
        if (!task.hasReminder || task.isCompleted) {
            cancelTaskReminder(context, task.id)
            return false
        }
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false

        val effectiveTimeStr = task.snoozedUntilTime?.takeIf { it.isNotBlank() }
            ?: task.reminderTime.takeIf { it.isNotBlank() }
            ?: task.startTime

        val triggerMillis = computeTriggerEpochMillis(task.dateIso, effectiveTimeStr) ?: return false
        if (triggerMillis <= System.currentTimeMillis()) return false

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TIME, task.startTime)
            putExtra(TaskReminderReceiver.EXTRA_TASK_CATEGORY, task.category.labelEn)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            true
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                true
            }.getOrDefault(false)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun computeTriggerEpochMillis(dateIso: String, hhMm: String): Long? {
        return try {
            val date = LocalDate.parse(dateIso)
            val mins = TimeUtils.parseMinutesOfDay(hhMm)
            val time = LocalTime.of(mins / 60, mins % 60)
            val dateTime = LocalDateTime.of(date, time)
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
