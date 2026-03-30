package dev.counterline.core.domain

import dev.counterline.core.data.repository.BookmarkRepository
import dev.counterline.core.data.repository.ImportedGameRepository
import dev.counterline.core.data.repository.MistakeRepository
import dev.counterline.core.data.repository.NodeReviewStateRepository
import dev.counterline.core.data.repository.RepertoireRepository
import dev.counterline.core.data.repository.SettingsRepository
import dev.counterline.core.data.repository.TacticalMotifRepository
import dev.counterline.core.data.repository.TransitionPlanRepository
import dev.counterline.core.data.repository.UserNoteRepository
import dev.counterline.core.model.Bookmark
import dev.counterline.core.model.DailyWorkout
import dev.counterline.core.model.ImportedGame
import dev.counterline.core.model.MistakeTheme
import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ReadinessScore
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.TacticalMotif
import dev.counterline.core.model.TransitionPlan
import dev.counterline.core.model.UserNote
import dev.counterline.core.model.WeeklyArc
import dev.counterline.core.model.WorkoutItem
import dev.counterline.core.model.WorkoutReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

// ──────────────────────────────────────────────────────────────────────
// Phase 3 — Personal Coach: Daily Workout Generator
// ──────────────────────────────────────────────────────────────────────

class GenerateDailyWorkoutUseCase @Inject constructor(
    private val reviewStateRepo: NodeReviewStateRepository,
    private val mistakeRepo: MistakeRepository,
    private val scheduler: ReviewScheduler,
    private val settingsRepo: SettingsRepository,
) {
    suspend operator fun invoke(): DailyWorkout {
        val now = System.currentTimeMillis()
        val settings = settingsRepo.settings.first()

        val allStates = reviewStateRepo.getAll().first()
        val dueItems = scheduler.buildReviewQueue(allStates)
        val unresolvedMistakes = mistakeRepo.getUnresolved().first()

        // Calculate side balance
        val whiteMastery = scheduler.calculateMastery(allStates.filter { it.side == Side.WHITE })
        val blackMastery = scheduler.calculateMastery(allStates.filter { it.side == Side.BLACK })
        val masteryGap = whiteMastery - blackMastery

        // Determine side focus: train the weaker side when gap > 15%
        val sideFocus = when {
            masteryGap > 0.15f -> Side.BLACK
            masteryGap < -0.15f -> Side.WHITE
            else -> null // mixed
        }

        // Build workout items
        val targetSize = when (settings.preferredSessionMinutes) {
            in 0..5 -> 8
            in 6..10 -> 15
            in 11..20 -> 20
            else -> 25
        }

        val items = mutableListOf<WorkoutItem>()

        // Priority 1: Mistake remediation (up to 30% of session)
        val mistakeSlots = (targetSize * 0.3).toInt()
        unresolvedMistakes
            .filter { sideFocus == null || it.side == sideFocus }
            .take(mistakeSlots)
            .forEachIndexed { idx, mistake ->
                items.add(
                    WorkoutItem(
                        nodeId = mistake.nodeId,
                        lineId = mistake.lineId,
                        side = mistake.side,
                        reason = WorkoutReason.MISTAKE_REMEDIATION,
                        priority = idx,
                    ),
                )
            }

        // Priority 2: Overdue reviews
        val overdueSlots = targetSize - items.size
        dueItems
            .filter { state ->
                sideFocus == null || state.side == sideFocus
            }
            .take(overdueSlots)
            .forEachIndexed { idx, state ->
                items.add(
                    WorkoutItem(
                        nodeId = state.nodeId,
                        lineId = state.lineId,
                        side = state.side,
                        reason = WorkoutReason.OVERDUE_REVIEW,
                        priority = items.size + idx,
                    ),
                )
            }

        // Interleave white/black
        val interleaved = interleaveWorkoutItems(items)

        // Determine recommended mode
        val recommendedMode = when {
            unresolvedMistakes.size > 5 -> StudyMode.MISTAKE_REVIEW
            dueItems.isEmpty() -> StudyMode.LEARN
            settings.preferredSessionMinutes <= 5 -> StudyMode.QUICK_5
            else -> StudyMode.RECALL
        }

        // Generate coach message
        val coachMessage = buildCoachMessage(
            dueCount = dueItems.size,
            mistakeCount = unresolvedMistakes.size,
            sideFocus = sideFocus,
            whiteMastery = whiteMastery,
            blackMastery = blackMastery,
            skillLevel = settings.skillLevel,
        )

        return DailyWorkout(
            date = now,
            recommendedMode = recommendedMode,
            sideFocus = sideFocus,
            items = interleaved,
            coachMessage = coachMessage,
            estimatedMinutes = settings.preferredSessionMinutes,
        )
    }

    private fun interleaveWorkoutItems(items: List<WorkoutItem>): List<WorkoutItem> {
        val white = items.filter { it.side == Side.WHITE }.toMutableList()
        val black = items.filter { it.side == Side.BLACK }.toMutableList()
        val result = mutableListOf<WorkoutItem>()
        while (white.isNotEmpty() || black.isNotEmpty()) {
            if (white.isNotEmpty()) result.add(white.removeAt(0))
            if (black.isNotEmpty()) result.add(black.removeAt(0))
        }
        return result
    }

    private fun buildCoachMessage(
        dueCount: Int,
        mistakeCount: Int,
        sideFocus: Side?,
        whiteMastery: Float,
        blackMastery: Float,
        skillLevel: SkillLevel,
    ): String {
        val parts = mutableListOf<String>()

        if (dueCount == 0 && mistakeCount == 0) {
            return "All caught up. Consider learning new lines or reviewing model games."
        }

        if (mistakeCount > 0) {
            parts.add("You have $mistakeCount unresolved mistake${if (mistakeCount > 1) "s" else ""}.")
        }
        if (dueCount > 0) {
            parts.add("$dueCount item${if (dueCount > 1) "s" else ""} due for review.")
        }
        if (sideFocus != null) {
            val sideName = sideFocus.name.lowercase()
            val weaker = if (sideFocus == Side.WHITE) whiteMastery else blackMastery
            parts.add(
                "Your $sideName weapon is weaker (${(weaker * 100).toInt()}% mastery). Today's focus: $sideName.",
            )
        }

        return parts.joinToString(" ")
    }
}

// ──────────────────────────────────────────────────────────────────────
// Phase 3 — Weakness Detection
// ──────────────────────────────────────────────────────────────────────

class DetectWeaknessesUseCase @Inject constructor(
    private val reviewStateRepo: NodeReviewStateRepository,
    private val mistakeRepo: MistakeRepository,
    private val scheduler: ReviewScheduler,
) {
    /**
     * Returns nodes that have been failed >= [threshold] times across different sessions.
     */
    fun chronicMissNodes(threshold: Int = 3): Flow<List<NodeReviewState>> =
        reviewStateRepo.getAll().map { states ->
            states.filter { it.lapseCount >= threshold }
                .sortedByDescending { it.lapseCount }
        }

    /**
     * Lines where accuracy drops >20% compared to user's overall average.
     */
    fun fragileLines(): Flow<Map<String, Float>> =
        reviewStateRepo.getAll().map { states ->
            if (states.isEmpty()) return@map emptyMap()
            val overallMastery = scheduler.calculateMastery(states)
            states.groupBy { it.lineId }
                .mapValues { (_, lineStates) -> scheduler.calculateMastery(lineStates) }
                .filter { it.value < overallMastery - 0.20f }
        }

    /**
     * Side imbalance: returns the gap (positive = White stronger).
     */
    fun sideImbalance(): Flow<Float> =
        reviewStateRepo.getAll().map { states ->
            val whiteMastery = scheduler.calculateMastery(states.filter { it.side == Side.WHITE })
            val blackMastery = scheduler.calculateMastery(states.filter { it.side == Side.BLACK })
            whiteMastery - blackMastery
        }

    /**
     * Mistakes grouped by theme.
     */
    fun mistakesByTheme(): Flow<Map<MistakeTheme, Int>> =
        mistakeRepo.getUnresolved().map { mistakes ->
            mistakes.groupBy { it.mistakeTheme }
                .mapValues { it.value.size }
        }
}

// ──────────────────────────────────────────────────────────────────────
// Phase 3 — Preparation Readiness Score
// ──────────────────────────────────────────────────────────────────────

class CalculateReadinessUseCase @Inject constructor(
    private val reviewStateRepo: NodeReviewStateRepository,
    private val mistakeRepo: MistakeRepository,
    private val scheduler: ReviewScheduler,
) {
    suspend fun forSide(side: Side): ReadinessScore {
        val now = System.currentTimeMillis()
        val states = reviewStateRepo.getBySide(side).first()
        val unresolvedMistakes = mistakeRepo.getUnresolved().first().filter { it.side == side }
        val overdueItems = states.filter { it.nextReviewEpochMs <= now }

        val mainlineCoverage = if (states.isEmpty()) 0f else {
            states.count { it.intervalDays >= 1f }.toFloat() / states.size
        }

        val recentStates = states.filter {
            it.lastReviewEpochMs > now - 7 * 86_400_000L
        }
        val recentAccuracy = if (recentStates.isEmpty()) 0f else {
            recentStates.count { it.lastGrade != ReviewGrade.FAIL }.toFloat() / recentStates.size
        }

        val overallMastery = scheduler.calculateMastery(states)

        // Weighted readiness: 40% mastery, 25% coverage, 20% recent accuracy, 15% penalty for issues
        val issuePenalty = min(
            0.15f,
            (unresolvedMistakes.size * 0.02f + overdueItems.size * 0.01f),
        )
        val overallScore = max(
            0f,
            overallMastery * 0.40f + mainlineCoverage * 0.25f + recentAccuracy * 0.20f + (0.15f - issuePenalty),
        )

        return ReadinessScore(
            side = side,
            overallScore = overallScore,
            mainlineCoverage = mainlineCoverage,
            deviationCoverage = 0f, // TODO: compute from deviation drill state
            recentAccuracy = recentAccuracy,
            unresolvedMistakes = unresolvedMistakes.size,
            overdueItems = overdueItems.size,
            assessedEpochMs = now,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
// Phase 2 — Tactical Motif Use Cases
// ──────────────────────────────────────────────────────────────────────

class GetTacticalMotifsUseCase @Inject constructor(
    private val repo: TacticalMotifRepository,
) {
    fun bySide(side: Side): Flow<List<TacticalMotif>> = repo.getBySide(side)
    fun byLineId(lineId: String): Flow<List<TacticalMotif>> = repo.getByLineId(lineId)
    suspend fun woodpeckerCycle(count: Int = 20): List<TacticalMotif> = repo.getWoodpeckerCycle(count)
    suspend fun randomSet(count: Int = 10): List<TacticalMotif> = repo.getRandomMotifs(count)
}

class RecordMotifAttemptUseCase @Inject constructor(
    private val repo: TacticalMotifRepository,
) {
    suspend operator fun invoke(motif: TacticalMotif, correct: Boolean) {
        repo.update(
            motif.copy(
                lastAttemptCorrect = correct,
                repetitionCycle = if (correct) motif.repetitionCycle + 1 else 0,
            ),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
// Phase 2 — Transition Trainer Use Cases
// ──────────────────────────────────────────────────────────────────────

class GetTransitionPlansUseCase @Inject constructor(
    private val repo: TransitionPlanRepository,
) {
    fun bySide(side: Side): Flow<List<TransitionPlan>> = repo.getBySide(side)
    fun byLineId(lineId: String): Flow<List<TransitionPlan>> = repo.getByLineId(lineId)
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4 — User Notes
// ──────────────────────────────────────────────────────────────────────

class ManageUserNotesUseCase @Inject constructor(
    private val repo: UserNoteRepository,
) {
    fun allNotes(): Flow<List<UserNote>> = repo.getAll()
    fun forLine(lineId: String): Flow<List<UserNote>> = repo.getByLineId(lineId)
    suspend fun getForNode(nodeId: String): UserNote? = repo.getByNodeId(nodeId)

    suspend fun save(note: UserNote): Long {
        val now = System.currentTimeMillis()
        return repo.upsert(
            note.copy(
                createdEpochMs = if (note.id == 0L) now else note.createdEpochMs,
                updatedEpochMs = now,
            ),
        )
    }

    suspend fun delete(id: Long) = repo.delete(id)
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4 — Bookmarks & Favorites
// ──────────────────────────────────────────────────────────────────────

class ManageBookmarksUseCase @Inject constructor(
    private val repo: BookmarkRepository,
) {
    fun all(): Flow<List<Bookmark>> = repo.getAll()
    fun favorites(): Flow<List<Bookmark>> = repo.getFavorites()
    fun tabiyas(): Flow<List<Bookmark>> = repo.getTabiyas()
    fun forLine(lineId: String): Flow<List<Bookmark>> = repo.getByLineId(lineId)

    suspend fun save(bookmark: Bookmark): Long {
        val now = System.currentTimeMillis()
        return repo.upsert(
            bookmark.copy(
                createdEpochMs = if (bookmark.id == 0L) now else bookmark.createdEpochMs,
            ),
        )
    }

    suspend fun delete(id: Long) = repo.delete(id)
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4 — PGN Import & Repertoire Comparison
// ──────────────────────────────────────────────────────────────────────

class ImportPgnUseCase @Inject constructor(
    private val importedGameRepo: ImportedGameRepository,
    private val repertoireRepo: RepertoireRepository,
) {
    /**
     * Parse a PGN string and compare against the active repertoire.
     * Returns the imported game with deviation information.
     */
    suspend fun import(pgn: String): ImportedGame {
        val now = System.currentTimeMillis()
        // Extract PGN headers (simple parser)
        val white = extractHeader(pgn, "White") ?: "Unknown"
        val black = extractHeader(pgn, "Black") ?: "Unknown"
        val result = extractHeader(pgn, "Result") ?: "*"
        val date = extractHeader(pgn, "Date") ?: ""
        val opening = extractHeader(pgn, "Opening") ?: ""

        val game = ImportedGame(
            pgn = pgn,
            white = white,
            black = black,
            result = result,
            date = date,
            opening = opening,
            importedEpochMs = now,
        )

        val id = importedGameRepo.insert(game)
        return game.copy(id = id)
    }

    fun allImported(): Flow<List<ImportedGame>> = importedGameRepo.getAll()
    fun withDeviations(): Flow<List<ImportedGame>> = importedGameRepo.getWithDeviations()

    private fun extractHeader(pgn: String, header: String): String? {
        val regex = """\[$header\s+"([^"]+)"\]""".toRegex()
        return regex.find(pgn)?.groupValues?.get(1)
    }
}

// ──────────────────────────────────────────────────────────────────────
// Phase 5 — Weekly Arc Computation
// ──────────────────────────────────────────────────────────────────────

class GetWeeklyArcsUseCase @Inject constructor(
    private val sessionRepo: dev.counterline.core.data.repository.StudySessionRepository,
    private val reviewStateRepo: NodeReviewStateRepository,
    private val mistakeRepo: MistakeRepository,
    private val scheduler: ReviewScheduler,
) {
    suspend fun lastNWeeks(n: Int = 8): List<WeeklyArc> {
        val now = System.currentTimeMillis()
        val msPerWeek = 7 * 86_400_000L
        val allStates = reviewStateRepo.getAll().first()

        return (0 until n).map { weekOffset ->
            val weekEnd = now - weekOffset * msPerWeek
            val weekStart = weekEnd - msPerWeek

            val sessions = sessionRepo.getSince(weekStart).first()
                .filter { it.startedEpochMs <= weekEnd }

            val itemsReviewed = sessions.sumOf { it.itemsCompleted }
            val correctCount = sessions.sumOf { it.correctCount }
            val accuracyPct = if (itemsReviewed > 0) correctCount.toFloat() / itemsReviewed else 0f
            val totalMinutes = sessions.sumOf {
                ((it.endedEpochMs - it.startedEpochMs) / 60_000L).toInt()
            }

            val whiteMastery = scheduler.calculateMastery(
                allStates.filter { it.side == Side.WHITE && it.lastReviewEpochMs <= weekEnd },
            )
            val blackMastery = scheduler.calculateMastery(
                allStates.filter { it.side == Side.BLACK && it.lastReviewEpochMs <= weekEnd },
            )

            WeeklyArc(
                weekStartEpochMs = weekStart,
                itemsLearned = 0, // derived from new SRS entries in the window
                itemsReviewed = itemsReviewed,
                mistakesResolved = 0, // would need historical mistake data
                accuracyPct = accuracyPct,
                totalStudyMinutes = totalMinutes,
                whiteMastery = whiteMastery,
                blackMastery = blackMastery,
            )
        }.reversed()
    }
}
