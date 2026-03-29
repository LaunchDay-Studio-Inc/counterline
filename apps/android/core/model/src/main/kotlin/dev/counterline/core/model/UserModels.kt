package dev.counterline.core.model

/** User's training progress for a specific drill or line */
data class UserProgress(
    val lineId: String,
    val drillId: String? = null,
    val correctCount: Int = 0,
    val totalAttempts: Int = 0,
    val lastReviewedEpochMs: Long = 0,
    val nextReviewEpochMs: Long = 0,
    val streakDays: Int = 0,
)

/** Aggregated stats for the progress dashboard */
data class ProgressStats(
    val totalDrillsCompleted: Int,
    val whiteLineAccuracy: Float,
    val blackLineAccuracy: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalStudyTimeMinutes: Int,
    val drillsCompletedToday: Int,
    val dueForReview: Int,
)

/** Settings stored in DataStore */
data class UserSettings(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val boardFlipped: Boolean = false,
    val dailyDrillGoal: Int = 10,
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 9,
)

enum class DarkMode { LIGHT, DARK, SYSTEM }
