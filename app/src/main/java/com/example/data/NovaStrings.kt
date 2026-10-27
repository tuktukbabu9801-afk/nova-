package com.example.data

import java.time.LocalTime

data class LocalizedCopy(
    val tagline: String,
    val greeting: String,
    val motivation: String,
    val navHome: String,
    val navTasks: String,
    val navPlanner: String,
    val navProgress: String,
    val navSettings: String,
    val addNewTask: String,
    val planWithAi: String,
    val planTomorrowBtn: String,
    val loadDemoBtn: String,
    val removeDemoBtn: String,
    val demoBannerText: String,
    val todayTasksLabel: String,
    val completedLabel: String,
    val pendingLabel: String,
    val completionRateLabel: String,
    val nextUpcomingTitle: String,
    val timelineTitle: String,
    val emptyTasksTitle: String,
    val emptyTasksSubtitle: String,
    val nightPlanQuote: String,
    val nightPlanTitle: String,
    val reschedulePromptPrefix: String,
    val aiPlannerHeader: String,
    val aiPlannerSubheader: String,
    val aiModeDisclosure: String
)

object NovaStrings {
    fun forLanguage(lang: AppLanguage, currentHour: Int = LocalTime.now().hour): LocalizedCopy {
        val timeGreetingEn = when (currentHour) {
            in 4..11 -> "Good morning!"
            in 12..16 -> "Good afternoon!"
            in 17..21 -> "Good evening!"
            else -> "Focused night session!"
        }
        val timeGreetingHi = when (currentHour) {
            in 4..11 -> "सुप्रभात! (Good morning!)"
            in 12..16 -> "नमस्ते! (Good afternoon!)"
            in 17..21 -> "शुभ संध्या! (Good evening!)"
            else -> "शांत रात्रि प्लानिंग!"
        }

        return when (lang) {
            AppLanguage.HINGLISH -> LocalizedCopy(
                tagline = "Plan Your Day. Take Control of Your Life.",
                greeting = timeGreetingEn,
                motivation = "Aaj ka din apne goals ke naam.",
                navHome = "Home",
                navTasks = "My Tasks",
                navPlanner = "AI Planner",
                navProgress = "Progress",
                navSettings = "Settings",
                addNewTask = "+ Add New Task",
                planWithAi = "Plan My Day with AI",
                planTomorrowBtn = "Plan Tomorrow (Night Prep)",
                loadDemoBtn = "Explore Sample Tasks",
                removeDemoBtn = "Remove Sample Tasks",
                demoBannerText = "SAMPLE MODE: Exploring demo schedule. Remove anytime to track your personal stats cleanly.",
                todayTasksLabel = "Today's Tasks",
                completedLabel = "Completed",
                pendingLabel = "Pending",
                completionRateLabel = "Daily Score",
                nextUpcomingTitle = "Next Up on Your Radar",
                timelineTitle = "Today's Chronological Timeline",
                emptyTasksTitle = "No tasks scheduled yet",
                emptyTasksSubtitle = "Aaj ka plan shuru karein! Tap '+ Add New Task' or 'Plan My Day with AI' to take control of your time.",
                nightPlanQuote = "Kal ko behtar banane ke liye aaj hi plan karo.",
                nightPlanTitle = "Night Planning — Plan Tomorrow",
                reschedulePromptPrefix = "Your task is still pending. Would you like to move it to",
                aiPlannerHeader = "NOVA Smart Daily Planner",
                aiPlannerSubheader = "Type your day in Hinglish, Hindi, or English — NOVA builds a conflict-free schedule.",
                aiModeDisclosure = "On-Device Smart Rule-Based Engine (Free & 100% Private). No data leaves your phone."
            )

            AppLanguage.ENGLISH -> LocalizedCopy(
                tagline = "Plan Your Day. Take Control of Your Life.",
                greeting = timeGreetingEn,
                motivation = "Dedicate today to your highest priorities.",
                navHome = "Home",
                navTasks = "My Tasks",
                navPlanner = "AI Planner",
                navProgress = "Progress",
                navSettings = "Settings",
                addNewTask = "+ Add New Task",
                planWithAi = "Plan My Day with AI",
                planTomorrowBtn = "Plan Tomorrow (Night Prep)",
                loadDemoBtn = "Explore Sample Tasks",
                removeDemoBtn = "Remove Sample Tasks",
                demoBannerText = "SAMPLE MODE: Exploring sample tasks. Remove them anytime to keep your personal stats separate.",
                todayTasksLabel = "Today's Tasks",
                completedLabel = "Completed",
                pendingLabel = "Pending",
                completionRateLabel = "Completion",
                nextUpcomingTitle = "Next Upcoming Task",
                timelineTitle = "Today's Schedule Timeline",
                emptyTasksTitle = "Your schedule is clear",
                emptyTasksSubtitle = "Create your first task or use the AI Planner to structure your day.",
                nightPlanQuote = "Plan tomorrow tonight for a calmer, more productive morning.",
                nightPlanTitle = "Night Planning — Plan Tomorrow",
                reschedulePromptPrefix = "Your task is still pending. Would you like to move it to",
                aiPlannerHeader = "NOVA Smart Daily Planner",
                aiPlannerSubheader = "Describe your goals in natural language and review a structured, conflict-free schedule.",
                aiModeDisclosure = "On-Device Smart Rule-Based Engine (Free & 100% Offline). Zero external data sharing."
            )

            AppLanguage.HINDI -> LocalizedCopy(
                tagline = "अपना दिन प्लान करें। अपनी ज़िंदगी को नियंत्रित करें।",
                greeting = timeGreetingHi,
                motivation = "आज का दिन अपने लक्ष्यों (goals) के नाम।",
                navHome = "होम",
                navTasks = "मेरे कार्य",
                navPlanner = "AI प्लानर",
                navProgress = "प्रगति",
                navSettings = "सेटिंग्स",
                addNewTask = "+ नया कार्य जोड़ें",
                planWithAi = "AI से दिन प्लान करें",
                planTomorrowBtn = "कल की योजना बनाएं",
                loadDemoBtn = "सैंपल कार्य देखें",
                removeDemoBtn = "सैंपल कार्य हटाएं",
                demoBannerText = "सैंपल मोड: ये उदाहरण कार्य हैं। अपने असली आँकड़ों के लिए इन्हें कभी भी हटा सकते हैं।",
                todayTasksLabel = "आज के कुल कार्य",
                completedLabel = "पूरे हुए",
                pendingLabel = "बाकी हैं",
                completionRateLabel = "आज की प्रगति",
                nextUpcomingTitle = "अगला आगामी कार्य",
                timelineTitle = "आज की समय-सारणी (Timeline)",
                emptyTasksTitle = "अभी कोई कार्य निर्धारित नहीं है",
                emptyTasksSubtitle = "अपना पहला कार्य जोड़ें या AI प्लानर की मदद से पूरे दिन का शेड्यूल बनाएं।",
                nightPlanQuote = "कल को बेहतर बनाने के लिए आज ही प्लान करो।",
                nightPlanTitle = "रात्रि प्लानिंग — कल की योजना",
                reschedulePromptPrefix = "आपका यह कार्य अभी बाकी है। क्या आप इसे इस समय पर शिफ्ट करना चाहेंगे:",
                aiPlannerHeader = "NOVA स्मार्ट डेली प्लानर",
                aiPlannerSubheader = "हिंदी, हिंग्लिश या अंग्रेज़ी में लिखें — NOVA आपके लिए समय-सारणी तैयार करेगा।",
                aiModeDisclosure = "ऑन-डिवाइस स्मार्ट रूल-बेस्ड इंजन (निःशुल्क एवं सुरक्षित)। आपका डेटा फोन पर ही रहता है।"
            )
        }
    }
}
