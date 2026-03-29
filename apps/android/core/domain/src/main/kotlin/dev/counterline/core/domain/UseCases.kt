package dev.counterline.core.domain

import dev.counterline.core.data.repository.BadgeRepository
import dev.counterline.core.data.repository.ClaimsRepository
import dev.counterline.core.data.repository.DeviationRepository
import dev.counterline.core.data.repository.DrillRepository
import dev.counterline.core.data.repository.ExamResultRepository
import dev.counterline.core.data.repository.MistakeRepository
import dev.counterline.core.data.repository.ModelGameRepository
import dev.counterline.core.data.repository.NodeReviewStateRepository
import dev.counterline.core.data.repository.PlanRepository
import dev.counterline.core.data.repository.QuickStartRepository
import dev.counterline.core.data.repository.RepertoireRepository
import dev.counterline.core.data.repository.SettingsRepository
import dev.counterline.core.data.repository.StudySessionRepository
import dev.counterline.core.data.repository.ThemeRepository
import dev.counterline.core.data.repository.UserProgressRepository
import dev.counterline.core.model.Badge
import dev.counterline.core.model.ClaimsManifest
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Drill
import dev.counterline.core.model.ExamResult
import dev.counterline.core.model.MistakeItem
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.Plan
import dev.counterline.core.model.ProofMatch
import dev.counterline.core.model.ProofSummary
import dev.counterline.core.model.QuickStart
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.StudySession
import dev.counterline.core.model.Theme
import dev.counterline.core.model.UserProgress
import dev.counterline.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRepertoireLinesUseCase @Inject constructor(
    private val repo: RepertoireRepository,
) {
    operator fun invoke(): Flow<List<RepertoireLine>> = repo.getAllLines()
    fun bySide(side: Side): Flow<List<RepertoireLine>> = repo.getLinesBySide(side)
    fun forSkillLevel(skillLevel: SkillLevel): Flow<List<RepertoireLine>> =
        repo.getAllLines().map { lines -> lines.filter { it.skillLevel <= skillLevel } }
}

class GetPlansUseCase @Inject constructor(
    private val repo: PlanRepository,
) {
    operator fun invoke(): Flow<List<Plan>> = repo.getAllPlans()
    fun bySide(side: Side): Flow<List<Plan>> = repo.getPlansBySide(side)
}

class GetThemesUseCase @Inject constructor(
    private val repo: ThemeRepository,
) {
    operator fun invoke(): Flow<List<Theme>> = repo.getAllThemes()
}

class GetDeviationsUseCase @Inject constructor(
    private val repo: DeviationRepository,
) {
    operator fun invoke(): Flow<List<Deviation>> = repo.getAllDeviations()
    fun bySide(side: Side): Flow<List<Deviation>> = repo.getDeviationsBySide(side)
}

class GetModelGamesUseCase @Inject constructor(
    private val repo: ModelGameRepository,
) {
    operator fun invoke(): Flow<List<ModelGame>> = repo.getAllGames()
}

class GetDrillsUseCase @Inject constructor(
    private val repo: DrillRepository,
) {
    operator fun invoke(): Flow<List<Drill>> = repo.getAllDrills()
    fun bySide(side: Side): Flow<List<Drill>> = repo.getDrillsBySide(side)
    fun forSkillLevel(skillLevel: SkillLevel): Flow<List<Drill>> =
        repo.getAllDrills().map { drills -> drills.filter { it.skillLevel <= skillLevel } }
}

class RecordDrillAttemptUseCase @Inject constructor(
    private val repo: UserProgressRepository,
) {
    suspend operator fun invoke(lineId: String, drillId: String, correct: Boolean) {
        repo.recordAttempt(lineId, drillId, correct)
    }
}

class GetProgressUseCase @Inject constructor(
    private val repo: UserProgressRepository,
) {
    operator fun invoke(): Flow<List<UserProgress>> = repo.getAllProgress()
    fun dueForReview(): Flow<List<UserProgress>> = repo.getDueForReview(System.currentTimeMillis())
    fun completedToday(dayStart: Long): Flow<Int> = repo.getDrillsCompletedToday(dayStart)
}

class GetClaimsUseCase @Inject constructor(
    private val repo: ClaimsRepository,
) {
    fun manifest(): ClaimsManifest = repo.getClaimsManifest()
    fun proofMatches(): List<ProofMatch> = repo.getProofMatches()
    fun proofSummary(): ProofSummary = repo.getProofSummary()
}

class GetQuickStartsUseCase @Inject constructor(
    private val repo: QuickStartRepository,
) {
    operator fun invoke(): Flow<List<QuickStart>> = repo.getAll()
}

class GetSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository,
) {
    operator fun invoke(): Flow<UserSettings> = repo.settings
}

class UpdateSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository,
) {
    suspend fun darkMode(mode: dev.counterline.core.model.DarkMode) = repo.updateDarkMode(mode)
    suspend fun boardFlipped(flipped: Boolean) = repo.updateBoardFlipped(flipped)
    suspend fun dailyDrillGoal(goal: Int) = repo.updateDailyDrillGoal(goal)
    suspend fun notifications(enabled: Boolean) = repo.updateNotificationsEnabled(enabled)
    suspend fun notificationHour(hour: Int) = repo.updateNotificationHour(hour)
    suspend fun skillLevel(level: SkillLevel) = repo.updateSkillLevel(level)
}

// --- NEW USE CASES FOR TRAINING SYSTEM ---

class ReviewNodeUseCase @Inject constructor(
    private val reviewStateRepo: NodeReviewStateRepository,
    private val mistakeRepo: MistakeRepository,
    private val scheduler: ReviewScheduler,
) {
    suspend operator fun invoke(
        nodeId: String,
        lineId: String,
        side: Side,
        grade: ReviewGrade,
        fen: String = "",
        expectedMove: String = "",
        userMove: String = "",
        explanation: String = "",
    ) {
        val current = reviewStateRepo.getByNodeId(nodeId) ?: NodeReviewState(
            nodeId = nodeId,
            side = side,
            lineId = lineId,
        )
        val updated = scheduler.schedule(current, grade)
        reviewStateRepo.upsert(updated)

        if (grade == ReviewGrade.FAIL && expectedMove.isNotEmpty()) {
            mistakeRepo.recordMistake(
                nodeId = nodeId,
                lineId = lineId,
                side = side,
                fen = fen,
                expectedMove = expectedMove,
                userMove = userMove,
                explanation = explanation,
            )
        }
    }
}

class GetReviewQueueUseCase @Inject constructor(
    private val repo: NodeReviewStateRepository,
    private val scheduler: ReviewScheduler,
) {
    fun dueNow(): Flow<List<NodeReviewState>> =
        repo.getDueForReview(System.currentTimeMillis())

    fun dueCount(): Flow<Int> = repo.getDueCount(System.currentTimeMillis())

    fun interleaved(maxItems: Int = 20): Flow<List<NodeReviewState>> =
        repo.getAll().map { scheduler.buildInterleavedSession(it, maxItems) }
}

class GetMistakesUseCase @Inject constructor(
    private val repo: MistakeRepository,
) {
    fun unresolved(): Flow<List<MistakeItem>> = repo.getUnresolved()
    fun dueForReview(): Flow<List<MistakeItem>> =
        repo.getDueForReview(System.currentTimeMillis())
    fun recent(limit: Int = 20): Flow<List<MistakeItem>> = repo.getRecent(limit)
    fun unresolvedCount(): Flow<Int> = repo.getUnresolvedCount()
    fun byLineId(lineId: String): Flow<List<MistakeItem>> = repo.getByLineId(lineId)
}

class ResolveMistakeUseCase @Inject constructor(
    private val repo: MistakeRepository,
) {
    suspend operator fun invoke(mistakeId: Long) = repo.markResolved(mistakeId)
}

class RecordExamResultUseCase @Inject constructor(
    private val examRepo: ExamResultRepository,
    private val badgeRepo: BadgeRepository,
) {
    suspend operator fun invoke(result: ExamResult): Long {
        val id = examRepo.insert(result)
        if (result.passed) {
            val badgeId = if (result.side == Side.WHITE) "white_exam_pass" else "black_exam_pass"
            badgeRepo.awardBadge(
                Badge(
                    id = badgeId,
                    title = if (result.side == Side.WHITE) "White Exam Certificate" else "Black Exam Certificate",
                    description = "Passed the ${result.side.name.lowercase()} repertoire exam with ${(result.accuracy * 100).toInt()}% accuracy",
                    side = result.side,
                    earnedEpochMs = System.currentTimeMillis(),
                ),
            )
        }
        return id
    }
}

class GetExamResultsUseCase @Inject constructor(
    private val repo: ExamResultRepository,
) {
    fun all(): Flow<List<ExamResult>> = repo.getAll()
    fun bySide(side: Side): Flow<List<ExamResult>> = repo.getBySide(side)
    suspend fun bestBySide(side: Side): ExamResult? = repo.getBestBySide(side)
}

class TrackStudySessionUseCase @Inject constructor(
    private val repo: StudySessionRepository,
) {
    suspend fun start(mode: StudyMode, side: Side? = null): Long =
        repo.startSession(mode, side)

    suspend fun end(sessionId: Long, itemsCompleted: Int, correctCount: Int) =
        repo.endSession(sessionId, itemsCompleted, correctCount)
}

class GetStudySessionsUseCase @Inject constructor(
    private val repo: StudySessionRepository,
) {
    fun all(): Flow<List<StudySession>> = repo.getAll()
    fun since(epochMs: Long): Flow<List<StudySession>> = repo.getSince(epochMs)
    fun totalStudyTimeMs(): Flow<Long?> = repo.getTotalStudyTimeMs()
    fun studyTimeMsByMode(mode: StudyMode): Flow<Long?> = repo.getStudyTimeMsByMode(mode)
    suspend fun currentStreak(): Int = repo.calculateStreak()
}

class GetMasteryUseCase @Inject constructor(
    private val reviewStateRepo: NodeReviewStateRepository,
    private val scheduler: ReviewScheduler,
) {
    fun forLine(lineId: String): Flow<Float> =
        reviewStateRepo.getByLineId(lineId).map { scheduler.calculateMastery(it) }

    fun forSide(side: Side): Flow<Float> =
        reviewStateRepo.getBySide(side).map { scheduler.calculateMastery(it) }

    fun weakestNodes(limit: Int = 10): Flow<List<NodeReviewState>> =
        reviewStateRepo.getWeakestNodes(limit)
}

class GetBadgesUseCase @Inject constructor(
    private val repo: BadgeRepository,
) {
    fun all(): Flow<List<Badge>> = repo.getAll()
    fun earned(): Flow<List<Badge>> = repo.getEarned()
}
