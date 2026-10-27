package com.example.planner

import com.example.data.NovaTask
import com.example.data.TaskCategory
import com.example.data.TaskPriority
import com.example.data.TimeUtils
import com.example.data.UserSettings
import java.time.LocalDate
import java.util.Locale

data class ProposedTaskItem(
    val id: Int,
    val title: String,
    val description: String,
    val suggestedDateIso: String,
    val suggestedStartTime: String,
    val estimatedDurationMinutes: Int,
    val priority: TaskPriority,
    val category: TaskCategory,
    val isBreak: Boolean = false,
    val conflictNote: String? = null
)

data class AiPlanProposal(
    val originalPrompt: String,
    val targetDateIso: String,
    val proposedTasks: List<ProposedTaskItem>,
    val assumptions: List<String>,
    val clarifyingQuestions: List<String>,
    val detectedConflictsResolved: Int,
    val engineLabel: String = "Local Rule-Based Scheduling Engine (Offline / Zero-Cost)"
)

object RuleBasedAiPlanner {

    /**
     * Parses natural language instructions in Hinglish, Hindi, or English into a structured,
     * conflict-free daily schedule while respecting explicit user dates/times and existing tasks.
     */
    fun generateScheduleFromPrompt(
        prompt: String,
        existingTasks: List<NovaTask>,
        settings: UserSettings = UserSettings(),
        todayIso: String = TimeUtils.todayIso(),
        regenerationSeed: Int = 0
    ): AiPlanProposal {
        val cleaned = prompt.trim()
        val lower = cleaned.lowercase(Locale.ROOT)
        val assumptions = mutableListOf<String>()
        val questions = mutableListOf<String>()

        // 1. Determine Target Date (Respect "kal" / "tomorrow" / "parso" / "aaj" / "today")
        val baseToday = runCatching { LocalDate.parse(todayIso) }.getOrDefault(LocalDate.now())
        val targetDateIso = when {
            lower.contains("parso") || lower.contains("day after tomorrow") -> {
                val d = baseToday.plusDays(2).toString()
                assumptions.add("Detected 'parso / day after tomorrow' — scheduled tasks for $d.")
                d
            }
            Regex("\\b(kal|tomorrow|tmrw)\\b").containsMatchIn(lower) || cleaned.contains("कल") -> {
                val d = baseToday.plusDays(1).toString()
                assumptions.add("Detected 'kal / tomorrow' — scheduled tasks for $d.")
                d
            }
            Regex("\\b(aaj|today)\\b").containsMatchIn(lower) || cleaned.contains("आज") -> {
                assumptions.add("Detected 'aaj / today' — scheduled tasks for $todayIso.")
                todayIso
            }
            else -> {
                assumptions.add("No explicit date mentioned — defaulted schedule date to Today ($todayIso).")
                todayIso
            }
        }

        // 2. Split clauses by natural conjunctions & punctuation in English, Hinglish, and Hindi
        val rawSegments = cleaned
            .split(Regex("(?:,|;|\\n|\\.|\\baur\\b|\\band\\b|\\bthen\\b|\\bphir\\b|\\bफिर\\b|\\bऔर\\b)"))
            .map { it.trim() }
            .filter { it.length >= 3 }

        val existingForDate = existingTasks.filter { it.dateIso == targetDateIso && !it.isCompleted }
        val occupiedRanges = mutableListOf<IntRange>()
        existingForDate.forEach { task ->
            val start = task.startMinutesOfDay()
            occupiedRanges.add(start until (start + task.durationMinutes))
        }

        val proposedItems = mutableListOf<ProposedTaskItem>()
        var conflictsResolved = 0
        val defaultStartMins = TimeUtils.parseMinutesOfDay(settings.defaultPlanningStartTime) + (regenerationSeed * 15)
        var rollingCursorMins = defaultStartMins.coerceIn(5 * 60, 22 * 60)
        var nextId = 1

        var explicitTimeFoundCount = 0

        for (segment in rawSegments) {
            val segLower = segment.lowercase(Locale.ROOT)
            // Skip pure date intro clauses like "kal mujhe" if they have no task substance
            if (segLower in listOf("kal", "aaj", "tomorrow", "today", "kal mujhe", "aaj mujhe")) continue

            val parsedTime = extractTimeAndDuration(segLower, rollingCursorMins, regenerationSeed)
            if (parsedTime.wasExplicitTime) {
                explicitTimeFoundCount++
            } else {
                assumptions.add("Assigned ${TimeUtils.formatMinutes12Hour(parsedTime.startMins)} (${parsedTime.durationMins}m) for '${ deriveCleanTitle(segment) }' based on time-of-day cues.")
            }

            val category = inferCategory(segLower, segment)
            val priority = inferPriority(segLower, category)
            val cleanTitle = deriveCleanTitle(segment)

            var chosenStartMins = parsedTime.startMins
            val durationMins = parsedTime.durationMins
            var conflictNote: String? = null

            // Check overlap with existing tasks or already-proposed tasks
            if (!parsedTime.wasExplicitTime) {
                // Find next conflict-free slot for flexible tasks
                while (hasOverlap(occupiedRanges, chosenStartMins, durationMins) && chosenStartMins + durationMins <= 23 * 60) {
                    chosenStartMins += 15
                    conflictsResolved++
                }
            } else {
                // Explicit time specified by user: DO NOT silently change it! Flag if it overlaps, or shift only if another proposed flexible task collided
                if (hasOverlap(occupiedRanges, chosenStartMins, durationMins)) {
                    conflictNote = "Note: Overlaps with an existing task at ${TimeUtils.formatMinutes12Hour(chosenStartMins)}. Kept your requested time so you can review."
                    conflictsResolved++
                }
            }

            occupiedRanges.add(chosenStartMins until (chosenStartMins + durationMins))
            rollingCursorMins = (chosenStartMins + durationMins + 15).coerceAtMost(22 * 60 + 30)

            proposedItems.add(
                ProposedTaskItem(
                    id = nextId++,
                    title = cleanTitle,
                    description = "Source instruction: \"$segment\"",
                    suggestedDateIso = targetDateIso,
                    suggestedStartTime = TimeUtils.minutesToHhMm(chosenStartMins),
                    estimatedDurationMinutes = durationMins,
                    priority = priority,
                    category = category,
                    isBreak = false,
                    conflictNote = conflictNote
                )
            )

            // Rule: Include reasonable breaks when appropriate (after >= 90 min Study/Work/Business blocks)
            if (durationMins >= 90 && (category == TaskCategory.STUDY || category == TaskCategory.WORK || category == TaskCategory.BUSINESS)) {
                val breakStart = chosenStartMins + durationMins
                val breakDuration = 15
                if (breakStart + breakDuration <= 23 * 60 && !hasOverlap(occupiedRanges, breakStart, breakDuration)) {
                    occupiedRanges.add(breakStart until (breakStart + breakDuration))
                    proposedItems.add(
                        ProposedTaskItem(
                            id = nextId++,
                            title = "Recharge & Hydration Break",
                            description = "Automatic 15-min recovery break after a ${durationMins}-minute deep focus session.",
                            suggestedDateIso = targetDateIso,
                            suggestedStartTime = TimeUtils.minutesToHhMm(breakStart),
                            estimatedDurationMinutes = breakDuration,
                            priority = TaskPriority.LOW,
                            category = TaskCategory.HEALTH,
                            isBreak = true
                        )
                    )
                    assumptions.add("Added a 15-minute recharge break after '$cleanTitle' (${durationMins}m focus block).")
                    rollingCursorMins = (breakStart + breakDuration + 15).coerceAtMost(22 * 60 + 30)
                }
            }
        }

        // Ask clarifying questions when critical information is missing
        if (explicitTimeFoundCount == 0 && proposedItems.isNotEmpty()) {
            questions.add("You didn't mention exact clock times — do these suggested start times fit your preferred routine?")
        }
        if (proposedItems.any { it.estimatedDurationMinutes == 60 } && !lower.contains("hour") && !lower.contains("ghante") && !lower.contains("min") && !Regex("\\bse\\b|\\bto\\b").containsMatchIn(lower)) {
            questions.add("How long would you like to spend on '${proposedItems.first().title}'? (Defaulted to 60 mins where not specified).")
        }
        if (proposedItems.size == 1 && cleaned.split(" ").size <= 3) {
            questions.add("Would you like to add more tasks for morning, afternoon, or evening to complete your daily plan?")
        }

        val sortedProposals = proposedItems.sortedBy { TimeUtils.parseMinutesOfDay(it.suggestedStartTime) }

        return AiPlanProposal(
            originalPrompt = cleaned,
            targetDateIso = targetDateIso,
            proposedTasks = sortedProposals,
            assumptions = assumptions.distinct(),
            clarifyingQuestions = questions.distinct(),
            detectedConflictsResolved = conflictsResolved
        )
    }

    /**
     * Suggests a conflict-free reschedule slot later on the same day (or 7:00 PM as default evening slot).
     */
    fun suggestRescheduleSlot(
        task: NovaTask,
        allTasksForSameDay: List<NovaTask>,
        currentMinutesOfDay: Int = TimeUtils.currentMinutesOfDay()
    ): String {
        val otherTasks = allTasksForSameDay.filter { it.id != task.id && !it.isCompleted }
        val occupied = otherTasks.map { it.startMinutesOfDay() until (it.startMinutesOfDay() + it.durationMinutes) }

        // Prefer 19:00 (7:00 PM) if the task was earlier and 7:00 PM is in the future & free
        val sevenPmMins = 19 * 60
        val taskStartMins = task.startMinutesOfDay()
        val baselineMins = maxOf(taskStartMins + 120, ((currentMinutesOfDay + 30) / 15) * 15)
        val firstCandidate = if (taskStartMins <= 17 * 60 && currentMinutesOfDay < sevenPmMins) {
            sevenPmMins
        } else {
            baselineMins.coerceAtMost(22 * 60)
        }

        var candidate = firstCandidate
        while (candidate + task.durationMinutes <= 23 * 60 + 30) {
            if (!hasOverlap(occupied, candidate, task.durationMinutes)) {
                return TimeUtils.minutesToHhMm(candidate)
            }
            candidate += 30
        }
        return TimeUtils.minutesToHhMm(firstCandidate.coerceIn(0, 23 * 60))
    }

    private fun hasOverlap(ranges: List<IntRange>, startMins: Int, durationMins: Int): Boolean {
        val endMins = startMins + durationMins
        return ranges.any { range -> startMins < (range.last + 1) && range.first < endMins }
    }

    private data class ExtractedTime(
        val startMins: Int,
        val durationMins: Int,
        val wasExplicitTime: Boolean
    )

    private fun extractTimeAndDuration(
        segLower: String,
        fallbackStartMins: Int,
        regenerationSeed: Int
    ): ExtractedTime {
        val periodHint = when {
            segLower.contains("subah") || segLower.contains("morning") || segLower.contains("am") || segLower.contains("सुबह") -> "AM"
            segLower.contains("dopahar") || segLower.contains("afternoon") || segLower.contains("दोपहर") -> "AFTERNOON"
            segLower.contains("shaam") || segLower.contains("evening") || segLower.contains("शाम") -> "PM"
            segLower.contains("raat") || segLower.contains("night") || segLower.contains("pm") || segLower.contains("रात") -> "NIGHT"
            else -> null
        }

        // 1. Check range pattern: e.g. "9 se 11 baje", "9 to 11", "9:30 se 11:00"
        val rangeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(?:am|pm)?\\s*(?:se|to|-)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(?:baje|am|pm)?")
        val rangeMatch = rangeRegex.find(segLower)
        if (rangeMatch != null) {
            val h1 = rangeMatch.groupValues[1].toIntOrNull() ?: 9
            val m1 = rangeMatch.groupValues[2].toIntOrNull() ?: 0
            val h2 = rangeMatch.groupValues[3].toIntOrNull() ?: (h1 + 1)
            val m2 = rangeMatch.groupValues[4].toIntOrNull() ?: 0

            val startH = normalizeHour(h1, periodHint, defaultMorningIfAmbiguous = h1 in 6..11)
            var endH = normalizeHour(h2, periodHint, defaultMorningIfAmbiguous = h2 in 7..11 && startH < 12)
            if (endH * 60 + m2 <= startH * 60 + m1) {
                endH += 12
            }
            val startMins = (startH * 60 + m1).coerceIn(0, 23 * 60)
            val duration = ((endH * 60 + m2) - startMins).coerceIn(15, 360)
            return ExtractedTime(startMins, duration, true)
        }

        // 2. Check explicit single time: e.g. "7 baje", "7:30 am", "5 pm", "7 बजे"
        val singleRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(baje|am|pm|बजे)")
        val singleMatch = singleRegex.find(segLower)
        val explicitDuration = extractExplicitDurationMinutes(segLower)

        if (singleMatch != null) {
            val rawH = singleMatch.groupValues[1].toIntOrNull() ?: 8
            val rawM = singleMatch.groupValues[2].toIntOrNull() ?: 0
            val marker = singleMatch.groupValues[3]
            val effectiveHint = when (marker) {
                "am" -> "AM"
                "pm" -> "PM"
                else -> periodHint
            }
            val hour24 = normalizeHour(rawH, effectiveHint, defaultMorningIfAmbiguous = rawH in 6..11)
            val startMins = (hour24 * 60 + rawM).coerceIn(0, 23 * 60 + 45)
            val dur = explicitDuration ?: defaultDurationForSegment(segLower)
            return ExtractedTime(startMins, dur, true)
        }

        // 3. Time-of-day anchor without clock digits (e.g. "dopahar ko business par kaam", "shaam ko exercise")
        val shift = (regenerationSeed % 3) * 30
        val anchorStart = when (periodHint) {
            "AM" -> 7 * 60 + shift
            "AFTERNOON" -> 14 * 60 + shift
            "PM" -> 18 * 60 + shift
            "NIGHT" -> 21 * 60
            else -> fallbackStartMins
        }
        val dur = explicitDuration ?: defaultDurationForSegment(segLower)
        return ExtractedTime(anchorStart.coerceIn(5 * 60, 22 * 60 + 30), dur, false)
    }

    private fun extractExplicitDurationMinutes(segLower: String): Int? {
        val minMatch = Regex("(\\d{1,3})\\s*(?:min|mins|minute|minutes|मिनट)").find(segLower)
        if (minMatch != null) {
            return minMatch.groupValues[1].toIntOrNull()?.coerceIn(10, 360)
        }
        val hrMatch = Regex("(\\d{1,2})\\s*(?:hr|hrs|hour|hours|ghanta|ghante|घंटे)").find(segLower)
        if (hrMatch != null) {
            val hrs = hrMatch.groupValues[1].toIntOrNull() ?: 1
            return (hrs * 60).coerceIn(30, 360)
        }
        return null
    }

    private fun defaultDurationForSegment(segLower: String): Int {
        return when {
            segLower.contains("uthna") || segLower.contains("wake") || segLower.contains("routine") -> 30
            segLower.contains("exercise") || segLower.contains("workout") || segLower.contains("gym") || segLower.contains("yoga") -> 45
            segLower.contains("padhai") || segLower.contains("study") || segLower.contains("business") -> 90
            else -> 60
        }
    }

    private fun normalizeHour(rawHour: Int, periodHint: String?, defaultMorningIfAmbiguous: Boolean): Int {
        val h = rawHour.coerceIn(0, 23)
        if (h > 12) return h
        return when (periodHint) {
            "AM" -> if (h == 12) 0 else h
            "AFTERNOON" -> if (h in 1..6) h + 12 else h
            "PM" -> if (h < 12) h + 12 else 12
            "NIGHT" -> if (h in 5..11) h + 12 else if (h == 12) 0 else h
            else -> if (defaultMorningIfAmbiguous) h else if (h in 1..6) h + 12 else h
        }
    }

    private fun inferCategory(segLower: String, rawSegment: String): TaskCategory {
        return when {
            Regex("padhai|study|exam|lecture|homework|revision|course|class|coding|पढ़ाई").containsMatchIn(segLower) || rawSegment.contains("पढ़ाई") -> TaskCategory.STUDY
            Regex("business|startup|client|sales|marketing|pitch|freelance|बिज़नेस").containsMatchIn(segLower) -> TaskCategory.BUSINESS
            Regex("exercise|gym|workout|fitness|run|walk|yoga|cardio|कसरत|व्यायाम").containsMatchIn(segLower) -> TaskCategory.FITNESS
            Regex("uthna|wake|sleep|meditat|water|breakfast|lunch|dinner|doctor|health|routine|उठना").containsMatchIn(segLower) -> TaskCategory.HEALTH
            Regex("office|meeting|work|project|email|report|kaam|काम").containsMatchIn(segLower) -> TaskCategory.WORK
            Regex("family|call|clean|home|market|grocery|personal|ghar").containsMatchIn(segLower) -> TaskCategory.PERSONAL
            else -> TaskCategory.OTHER
        }
    }

    private fun inferPriority(segLower: String, category: TaskCategory): TaskPriority {
        return when {
            Regex("urgent|important|high|zaroori|must|deadline|exam").containsMatchIn(segLower) -> TaskPriority.HIGH
            Regex("low|optional|maybe|relax").containsMatchIn(segLower) -> TaskPriority.LOW
            category == TaskCategory.STUDY || category == TaskCategory.BUSINESS || category == TaskCategory.WORK -> TaskPriority.HIGH
            else -> TaskPriority.MEDIUM
        }
    }

    private fun deriveCleanTitle(rawSegment: String): String {
        val lower = rawSegment.lowercase(Locale.ROOT)
        // Friendly canonical titles for common Hinglish phrases while keeping specifics
        when {
            (lower.contains("uthna") || lower.contains("wake up")) && !lower.contains("padhai") ->
                return "Wake Up & Morning Routine"
            lower.contains("padhai") && !lower.contains("business") ->
                return "Focused Study Session (Padhai)"
            lower.contains("business") && lower.contains("kaam") ->
                return "Work on Business & Growth"
            (lower.contains("exercise") || lower.contains("workout") || lower.contains("gym")) && lower.length < 35 ->
                return "Evening Exercise & Workout"
        }

        // Strip common Hinglish time/date boilerplate to produce a clean, readable task title
        var cleaned = rawSegment
            .replace(Regex("(?i)\\b(kal|aaj|parso|tomorrow|today|subah|dopahar|shaam|raat|ko|par|pe|mein|tak|se)\\b"), " ")
            .replace(Regex("(?i)\\b\\d{1,2}(?::\\d{2})?\\s*(?:baje|am|pm)?\\b"), " ")
            .replace(Regex("(?i)\\b(karni hai|karna hai|hoga|hai|mujhe|i need to|i want to)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.length < 3) {
            cleaned = rawSegment.trim()
        }
        return cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
