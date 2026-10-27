package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TaskPriority(val labelEn: String, val labelHi: String, val weight: Int) {
    HIGH("High", "उच्च (High)", 3),
    MEDIUM("Medium", "मध्यम (Medium)", 2),
    LOW("Low", "सामान्य (Low)", 1)
}

enum class TaskCategory(val labelEn: String, val labelHi: String, val emoji: String) {
    STUDY("Study", "पढ़ाई (Study)", "📚"),
    WORK("Work", "काम (Work)", "💼"),
    BUSINESS("Business", "बिज़नेस (Business)", "🚀"),
    FITNESS("Fitness", "फ़िटनेस (Fitness)", "💪"),
    HEALTH("Health", "स्वास्थ्य (Health)", "🌿"),
    PERSONAL("Personal", "व्यक्तिगत (Personal)", "🏠"),
    OTHER("Other", "अन्य (Other)", "✨")
}

enum class RecurringType(val labelEn: String, val labelHi: String) {
    NONE("One-time", "एक बार (One-time)"),
    DAILY("Daily", "रोज़ाना (Daily)"),
    WEEKDAYS("Weekdays (Mon-Fri)", "सोम-शुक्र (Weekdays)"),
    WEEKLY("Weekly", "साप्ताहिक (Weekly)")
}

enum class AppLanguage(val code: String, val displayName: String) {
    HINGLISH("hinglish", "Hinglish (Default)"),
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी (Hindi)")
}

enum class TaskFilter {
    ALL,
    TODAY,
    PENDING,
    COMPLETED,
    OVERDUE,
    UPCOMING
}

@Entity(tableName = "nova_tasks")
data class NovaTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dateIso: String, // YYYY-MM-DD
    val startTime: String, // HH:mm (24h)
    val durationMinutes: Int = 60,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.OTHER,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val hasReminder: Boolean = false,
    val reminderTime: String = "", // HH:mm (24h)
    val recurring: RecurringType = RecurringType.NONE,
    val isSample: Boolean = false,
    val snoozedUntilTime: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun startMinutesOfDay(): Int = TimeUtils.parseMinutesOfDay(startTime)

    fun endMinutesOfDay(): Int = (startMinutesOfDay() + durationMinutes).coerceAtMost(24 * 60 - 1)

    fun formattedTimeRange(): String {
        val startFormatted = TimeUtils.format12Hour(startTime)
        val endFormatted = TimeUtils.formatMinutes12Hour(endMinutesOfDay())
        return "$startFormatted – $endFormatted"
    }

    fun formattedStartTime(): String = TimeUtils.format12Hour(startTime)

    fun isOverdue(todayIso: String, currentMinutesOfDay: Int): Boolean {
        if (isCompleted) return false
        return when {
            dateIso < todayIso -> true
            dateIso == todayIso -> endMinutesOfDay() < currentMinutesOfDay
            else -> false
        }
    }

    fun overlapsWith(otherDateIso: String, otherStartMinutes: Int, otherDurationMinutes: Int): Boolean {
        if (dateIso != otherDateIso) return false
        val aStart = startMinutesOfDay()
        val aEnd = aStart + durationMinutes
        val bStart = otherStartMinutes
        val bEnd = bStart + otherDurationMinutes
        return aStart < bEnd && bStart < aEnd
    }
}

data class UserSettings(
    val language: AppLanguage = AppLanguage.HINGLISH,
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val defaultPlanningStartTime: String = "07:00",
    val workingHoursStart: String = "09:00",
    val workingHoursEnd: String = "21:00",
    val reminderLeadMinutes: Int = 10
)

object TimeUtils {
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)

    fun todayIso(): String = LocalDate.now().format(isoDateFormatter)

    fun tomorrowIso(): String = LocalDate.now().plusDays(1).format(isoDateFormatter)

    fun currentMinutesOfDay(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    fun parseMinutesOfDay(hhMm: String): Int {
        val parts = hhMm.trim().split(":")
        if (parts.size != 2) return 9 * 60
        val h = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: 0
        return h * 60 + m
    }

    fun minutesToHhMm(totalMinutes: Int): String {
        val clamped = totalMinutes.coerceIn(0, 23 * 60 + 59)
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun format12Hour(hhMm: String): String {
        return formatMinutes12Hour(parseMinutesOfDay(hhMm))
    }

    fun formatMinutes12Hour(totalMinutes: Int): String {
        val clamped = totalMinutes.coerceIn(0, 23 * 60 + 59)
        val h24 = clamped / 60
        val m = clamped % 60
        val period = if (h24 < 12) "AM" else "PM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format(Locale.US, "%d:%02d %s", h12, m, period)
    }

    fun formatReadableDate(dateIso: String, todayIso: String = todayIso()): String {
        return try {
            val date = LocalDate.parse(dateIso, isoDateFormatter)
            val today = LocalDate.parse(todayIso, isoDateFormatter)
            when (date) {
                today -> "Today (${date.format(displayDateFormatter)})"
                today.plusDays(1) -> "Tomorrow (${date.format(displayDateFormatter)})"
                today.minusDays(1) -> "Yesterday (${date.format(displayDateFormatter)})"
                else -> date.format(displayDateFormatter)
            }
        } catch (_: Exception) {
            dateIso
        }
    }

    fun isValidIsoDate(dateIso: String): Boolean {
        return try {
            LocalDate.parse(dateIso.trim(), isoDateFormatter)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isValidHhMm(hhMm: String): Boolean {
        val regex = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")
        return regex.matches(hhMm.trim())
    }
}
