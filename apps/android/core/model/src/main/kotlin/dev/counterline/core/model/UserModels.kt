package dev.counterline.core.model

/** Per-node spaced repetition review state */
data class NodeReviewState(
    val nodeId: String,
    val side: Side,
    val lineId: String,
    val easeFactor: Float = 2.5f,
    val intervalDays: Float = 0f,
    val repetitions: Int = 0,
    val lastReviewEpochMs: Long = 0,
    val nextReviewEpochMs: Long = 0,
    val lapseCount: Int = 0,
    val lastGrade: ReviewGrade = ReviewGrade.GOOD,
)

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

/** A missed move that becomes a review item */
data class MistakeItem(
    val id: Long = 0,
    val nodeId: String,
    val lineId: String,
    val side: Side,
    val fen: String,
    val expectedMove: String,
    val userMove: String,
    val explanation: String,
    val createdEpochMs: Long,
    val nextReviewEpochMs: Long,
    val reviewCount: Int = 0,
    val resolved: Boolean = false,
    val mistakeTheme: MistakeTheme = MistakeTheme.MOVE_ORDER,
    val severity: MistakeSeverity = MistakeSeverity.MINOR,
    val consecutiveCorrect: Int = 0,
)

/** Classification of mistake type for grouped review */
enum class MistakeTheme {
    MOVE_ORDER,
    TACTICAL_MISS,
    STRATEGIC_MISUNDERSTANDING,
    DEVIATION_UNFAMILIARITY,
    TRANSPOSITION_ERROR,
}

/** Severity level affecting remediation priority */
enum class MistakeSeverity {
    MINOR,
    MODERATE,
    CRITICAL,
}

/** Exam result record */
data class ExamResult(
    val id: Long = 0,
    val side: Side,
    val startedEpochMs: Long,
    val finishedEpochMs: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val avgResponseTimeMs: Long,
    val branchCoverage: Float,
    val passed: Boolean,
)

/** Study session record for time tracking */
data class StudySession(
    val id: Long = 0,
    val mode: StudyMode,
    val side: Side? = null,
    val startedEpochMs: Long,
    val endedEpochMs: Long = 0,
    val itemsCompleted: Int = 0,
    val correctCount: Int = 0,
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
    val whiteMastery: Float = 0f,
    val blackMastery: Float = 0f,
    val weakestNodes: List<String> = emptyList(),
    val mostMissedDeviations: List<String> = emptyList(),
    val accuracyTrend: List<Float> = emptyList(),
    val timeByMode: Map<StudyMode, Int> = emptyMap(),
    val whiteReadiness: Float = 0f,
    val blackReadiness: Float = 0f,
    val unresolvedMistakeCount: Int = 0,
    val mistakesByTheme: Map<MistakeTheme, Int> = emptyMap(),
    val weeklyArcs: List<WeeklyArc> = emptyList(),
)

/** Per-opening mastery map entry */
data class OpeningMastery(
    val lineId: String,
    val lineName: String,
    val side: Side,
    val masteryScore: Float,
    val nodesTotal: Int,
    val nodesMastered: Int,
    val nodesDue: Int,
    val nodesLapsed: Int,
)

/** Badge / certificate earned in-app */
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val side: Side? = null,
    val earnedEpochMs: Long? = null,
    val iconName: String = "star",
)

/** Settings stored in DataStore */
data class UserSettings(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val boardFlipped: Boolean = false,
    val dailyDrillGoal: Int = 10,
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
    val tournamentDate: Long? = null,
    val preferredSessionMinutes: Int = 15,
    val blindfoldModeEnabled: Boolean = false,
    val autoInterleave: Boolean = true,
)

enum class DarkMode { LIGHT, DARK, SYSTEM }
