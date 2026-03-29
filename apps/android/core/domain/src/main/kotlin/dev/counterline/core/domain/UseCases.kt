package dev.counterline.core.domain

import dev.counterline.core.data.repository.ClaimsRepository
import dev.counterline.core.data.repository.DeviationRepository
import dev.counterline.core.data.repository.DrillRepository
import dev.counterline.core.data.repository.ModelGameRepository
import dev.counterline.core.data.repository.PlanRepository
import dev.counterline.core.data.repository.QuickStartRepository
import dev.counterline.core.data.repository.RepertoireRepository
import dev.counterline.core.data.repository.SettingsRepository
import dev.counterline.core.data.repository.ThemeRepository
import dev.counterline.core.data.repository.UserProgressRepository
import dev.counterline.core.model.ClaimsManifest
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Drill
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.Plan
import dev.counterline.core.model.ProofMatch
import dev.counterline.core.model.ProofSummary
import dev.counterline.core.model.QuickStart
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.Side
import dev.counterline.core.model.Theme
import dev.counterline.core.model.UserProgress
import dev.counterline.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRepertoireLinesUseCase @Inject constructor(
    private val repo: RepertoireRepository,
) {
    operator fun invoke(): Flow<List<RepertoireLine>> = repo.getAllLines()
    fun bySide(side: Side): Flow<List<RepertoireLine>> = repo.getLinesBySide(side)
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
}
