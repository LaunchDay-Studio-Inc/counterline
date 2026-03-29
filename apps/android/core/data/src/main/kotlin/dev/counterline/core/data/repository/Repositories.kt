package dev.counterline.core.data.repository

import dev.counterline.core.database.dao.BadgeDao
import dev.counterline.core.database.dao.DeviationDao
import dev.counterline.core.database.dao.DrillDao
import dev.counterline.core.database.dao.ExamResultDao
import dev.counterline.core.database.dao.MistakeItemDao
import dev.counterline.core.database.dao.ModelGameDao
import dev.counterline.core.database.dao.NodeReviewStateDao
import dev.counterline.core.database.dao.PiecePlacementDao
import dev.counterline.core.database.dao.PlanDao
import dev.counterline.core.database.dao.QuickStartDao
import dev.counterline.core.database.dao.RepertoireDao
import dev.counterline.core.database.dao.StudySessionDao
import dev.counterline.core.database.dao.ThemeDao
import dev.counterline.core.database.dao.UserProgressDao
import dev.counterline.core.database.entity.BadgeEntity
import dev.counterline.core.database.entity.ExamResultEntity
import dev.counterline.core.database.entity.MistakeItemEntity
import dev.counterline.core.database.entity.NodeReviewStateEntity
import dev.counterline.core.database.entity.StudySessionEntity
import dev.counterline.core.database.entity.UserProgressEntity
import dev.counterline.core.model.Badge
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Drill
import dev.counterline.core.model.DrillType
import dev.counterline.core.model.ExamResult
import dev.counterline.core.model.GameAnnotation
import dev.counterline.core.model.MistakeItem
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.PiecePlacement
import dev.counterline.core.model.Plan
import dev.counterline.core.model.QuickStart
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.StudySession
import dev.counterline.core.model.Theme
import dev.counterline.core.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepertoireRepository @Inject constructor(
    private val repertoireDao: RepertoireDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllLines(): Flow<List<RepertoireLine>> =
        repertoireDao.getAllLines().map { entities ->
            entities.map { entity ->
                val moves = repertoireDao.getMovesForLine(entity.id)
                RepertoireLine(
                    id = entity.id,
                    name = entity.name,
                    family = entity.family,
                    eco = entity.eco,
                    side = Side.valueOf(entity.side),
                    seedLine = entity.seedLine,
                    exitFen = entity.exitFen,
                    exitEpd = entity.exitEpd,
                    exitMoveNumber = entity.exitMoveNumber,
                    specialistType = entity.specialistType,
                    specialistSize = entity.specialistSize,
                    screeningRank = entity.screeningRank,
                    screeningScorePct = entity.screeningScorePct,
                    evaluationAtExit = entity.evaluationAtExit,
                    moves = moves.map { m ->
                        RepertoireMove(m.moveNumber, m.san, m.purpose, m.isWhiteMove, m.whyThisMove, m.keyPlanCallout)
                    },
                    memoryHook = entity.memoryHook,
                    memoryHookBreakdown = json.decodeFromString(entity.memoryHookBreakdownJson),
                    skillLevel = SkillLevel.valueOf(entity.skillLevel),
                )
            }
        }

    fun getLinesBySide(side: Side): Flow<List<RepertoireLine>> =
        repertoireDao.getLinesBySide(side.name).map { entities ->
            entities.map { entity ->
                val moves = repertoireDao.getMovesForLine(entity.id)
                RepertoireLine(
                    id = entity.id,
                    name = entity.name,
                    family = entity.family,
                    eco = entity.eco,
                    side = Side.valueOf(entity.side),
                    seedLine = entity.seedLine,
                    exitFen = entity.exitFen,
                    exitEpd = entity.exitEpd,
                    exitMoveNumber = entity.exitMoveNumber,
                    specialistType = entity.specialistType,
                    specialistSize = entity.specialistSize,
                    screeningRank = entity.screeningRank,
                    screeningScorePct = entity.screeningScorePct,
                    evaluationAtExit = entity.evaluationAtExit,
                    moves = moves.map { m ->
                        RepertoireMove(m.moveNumber, m.san, m.purpose, m.isWhiteMove, m.whyThisMove, m.keyPlanCallout)
                    },
                    memoryHook = entity.memoryHook,
                    memoryHookBreakdown = json.decodeFromString(entity.memoryHookBreakdownJson),
                    skillLevel = SkillLevel.valueOf(entity.skillLevel),
                )
            }
        }
}

@Singleton
class PlanRepository @Inject constructor(private val planDao: PlanDao) {
    fun getAllPlans(): Flow<List<Plan>> = planDao.getAllPlans().map { entities ->
        entities.map { Plan(it.id, Side.valueOf(it.side), it.title, it.description, it.priority, SkillLevel.valueOf(it.skillLevel)) }
    }

    fun getPlansBySide(side: Side): Flow<List<Plan>> = planDao.getPlansBySide(side.name).map { entities ->
        entities.map { Plan(it.id, Side.valueOf(it.side), it.title, it.description, it.priority, SkillLevel.valueOf(it.skillLevel)) }
    }
}

@Singleton
class ThemeRepository @Inject constructor(private val themeDao: ThemeDao) {
    fun getAllThemes(): Flow<List<Theme>> = themeDao.getAllThemes().map { entities ->
        entities.map { Theme(it.id, Side.valueOf(it.side), it.title, it.description, it.occurrenceRate, SkillLevel.valueOf(it.skillLevel)) }
    }
}

@Singleton
class PiecePlacementRepository @Inject constructor(private val dao: PiecePlacementDao) {
    fun getPlacementsBySide(side: Side): Flow<List<PiecePlacement>> =
        dao.getPlacementsBySide(side.name).map { entities ->
            entities.map { PiecePlacement(it.piece, it.idealSquare, it.purpose, Side.valueOf(it.side)) }
        }
}

@Singleton
class DeviationRepository @Inject constructor(private val deviationDao: DeviationDao) {
    fun getAllDeviations(): Flow<List<Deviation>> = deviationDao.getAllDeviations().map { entities ->
        entities.map {
            Deviation(it.id, Side.valueOf(it.side), it.deviationName, it.move, it.description, it.response, it.strategicIdea, SkillLevel.valueOf(it.skillLevel))
        }
    }

    fun getDeviationsBySide(side: Side): Flow<List<Deviation>> =
        deviationDao.getDeviationsBySide(side.name).map { entities ->
            entities.map {
                Deviation(it.id, Side.valueOf(it.side), it.deviationName, it.move, it.description, it.response, it.strategicIdea, SkillLevel.valueOf(it.skillLevel))
            }
        }
}

@Singleton
class ModelGameRepository @Inject constructor(private val modelGameDao: ModelGameDao) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllGames(): Flow<List<ModelGame>> = modelGameDao.getAllGames().map { entities ->
        entities.map { e ->
            ModelGame(
                id = e.id, title = e.title, side = Side.valueOf(e.side),
                opening = e.opening, result = e.result, moveCount = e.moveCount,
                keyTheme = e.keyTheme,
                annotations = json.decodeFromString<List<GameAnnotation>>(e.annotationsJson),
                evaluationProgression = e.evaluationProgression,
            )
        }
    }
}

@Singleton
class DrillRepository @Inject constructor(private val drillDao: DrillDao) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllDrills(): Flow<List<Drill>> = drillDao.getAllDrills().map { entities ->
        entities.map { e ->
            Drill(
                id = e.id, type = DrillType.valueOf(e.type), title = e.title,
                question = e.question,
                options = e.optionsJson?.let { json.decodeFromString<List<String>>(it) },
                correctAnswer = e.correctAnswer, explanation = e.explanation,
                side = e.side?.let { Side.valueOf(it) }, fen = e.fen,
                skillLevel = SkillLevel.valueOf(e.skillLevel), lineId = e.lineId,
            )
        }
    }

    fun getDrillsBySide(side: Side): Flow<List<Drill>> =
        drillDao.getDrillsBySide(side.name).map { entities ->
            entities.map { e ->
                Drill(
                    id = e.id, type = DrillType.valueOf(e.type), title = e.title,
                    question = e.question,
                    options = e.optionsJson?.let { json.decodeFromString<List<String>>(it) },
                    correctAnswer = e.correctAnswer, explanation = e.explanation,
                    side = e.side?.let { Side.valueOf(it) }, fen = e.fen,
                    skillLevel = SkillLevel.valueOf(e.skillLevel), lineId = e.lineId,
                )
            }
        }
}

@Singleton
class UserProgressRepository @Inject constructor(private val dao: UserProgressDao) {
    fun getAllProgress(): Flow<List<UserProgress>> = dao.getAllProgress().map { entities ->
        entities.map { it.toModel() }
    }

    fun getDueForReview(now: Long): Flow<List<UserProgress>> = dao.getDueForReview(now).map { entities ->
        entities.map { it.toModel() }
    }

    fun getDrillsCompletedToday(dayStart: Long): Flow<Int> = dao.getDrillsCompletedToday(dayStart)

    suspend fun recordAttempt(
        lineId: String,
        drillId: String,
        correct: Boolean,
    ) {
        val existing = dao.getProgressForDrill(drillId)
        val now = System.currentTimeMillis()

        if (existing != null) {
            val updated = existing.copy(
                correctCount = existing.correctCount + if (correct) 1 else 0,
                totalAttempts = existing.totalAttempts + 1,
                lastReviewedEpochMs = now,
                nextReviewEpochMs = calculateNextReview(
                    existing.correctCount + if (correct) 1 else 0,
                    existing.totalAttempts + 1,
                ),
            )
            dao.upsertProgress(updated)
        } else {
            dao.upsertProgress(
                UserProgressEntity(
                    lineId = lineId,
                    drillId = drillId,
                    correctCount = if (correct) 1 else 0,
                    totalAttempts = 1,
                    lastReviewedEpochMs = now,
                    nextReviewEpochMs = calculateNextReview(
                        if (correct) 1 else 0, 1,
                    ),
                    streakDays = if (correct) 1 else 0,
                ),
            )
        }
    }

    suspend fun getLongestStreak(): Int = dao.getLongestStreak() ?: 0

    private fun calculateNextReview(correct: Int, total: Int): Long {
        // Simple spaced repetition: interval doubles with each correct answer
        val accuracy = if (total > 0) correct.toDouble() / total else 0.0
        val baseIntervalMs = 24 * 60 * 60 * 1000L // 1 day
        val multiplier = when {
            accuracy >= 0.9 -> 4
            accuracy >= 0.7 -> 2
            accuracy >= 0.5 -> 1
            else -> 0 // review immediately
        }
        return System.currentTimeMillis() + baseIntervalMs * multiplier
    }

    private fun UserProgressEntity.toModel() = UserProgress(
        lineId = lineId,
        drillId = drillId,
        correctCount = correctCount,
        totalAttempts = totalAttempts,
        lastReviewedEpochMs = lastReviewedEpochMs,
        nextReviewEpochMs = nextReviewEpochMs,
        streakDays = streakDays,
    )
}

@Singleton
class QuickStartRepository @Inject constructor(private val dao: QuickStartDao) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): Flow<List<QuickStart>> = dao.getAll().map { entities ->
        entities.map { e ->
            QuickStart(
                side = Side.valueOf(e.side),
                lineName = e.lineName,
                seedLine = e.seedLine,
                memoryHook = e.memoryHook,
                memoryHookBreakdown = json.decodeFromString(e.memoryHookBreakdownJson),
                threeKeyActions = json.decodeFromString(e.threeKeyActionsJson),
                exitFen = e.exitFen,
                exitEvaluation = e.exitEvaluation,
                typicalResult = e.typicalResult,
            )
        }
    }
}

// --- NEW REPOSITORIES FOR TRAINING SYSTEM ---

@Singleton
class NodeReviewStateRepository @Inject constructor(
    private val dao: NodeReviewStateDao,
) {
    fun getAll(): Flow<List<NodeReviewState>> = dao.getAll().map { entities ->
        entities.map { it.toModel() }
    }

    fun getByLineId(lineId: String): Flow<List<NodeReviewState>> =
        dao.getByLineId(lineId).map { entities -> entities.map { it.toModel() } }

    fun getBySide(side: Side): Flow<List<NodeReviewState>> =
        dao.getBySide(side.name).map { entities -> entities.map { it.toModel() } }

    fun getDueForReview(now: Long): Flow<List<NodeReviewState>> =
        dao.getDueForReview(now).map { entities -> entities.map { it.toModel() } }

    fun getDueCount(now: Long): Flow<Int> = dao.getDueCount(now)

    fun getWeakestNodes(limit: Int = 10): Flow<List<NodeReviewState>> =
        dao.getWeakestNodes(limit).map { entities -> entities.map { it.toModel() } }

    suspend fun getByNodeId(nodeId: String): NodeReviewState? =
        dao.getByNodeId(nodeId)?.toModel()

    suspend fun upsert(state: NodeReviewState) = dao.upsert(state.toEntity())

    suspend fun upsertAll(states: List<NodeReviewState>) =
        dao.upsertAll(states.map { it.toEntity() })

    private fun NodeReviewStateEntity.toModel() = NodeReviewState(
        nodeId = nodeId,
        side = Side.valueOf(side),
        lineId = lineId,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetitions = repetitions,
        lastReviewEpochMs = lastReviewEpochMs,
        nextReviewEpochMs = nextReviewEpochMs,
        lapseCount = lapseCount,
        lastGrade = ReviewGrade.valueOf(lastGrade),
    )

    private fun NodeReviewState.toEntity() = NodeReviewStateEntity(
        nodeId = nodeId,
        side = side.name,
        lineId = lineId,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetitions = repetitions,
        lastReviewEpochMs = lastReviewEpochMs,
        nextReviewEpochMs = nextReviewEpochMs,
        lapseCount = lapseCount,
        lastGrade = lastGrade.name,
    )
}

@Singleton
class MistakeRepository @Inject constructor(
    private val dao: MistakeItemDao,
) {
    fun getUnresolved(): Flow<List<MistakeItem>> =
        dao.getUnresolved().map { entities -> entities.map { it.toModel() } }

    fun getDueForReview(now: Long): Flow<List<MistakeItem>> =
        dao.getDueForReview(now).map { entities -> entities.map { it.toModel() } }

    fun getByLineId(lineId: String): Flow<List<MistakeItem>> =
        dao.getByLineId(lineId).map { entities -> entities.map { it.toModel() } }

    fun getRecent(limit: Int = 20): Flow<List<MistakeItem>> =
        dao.getRecent(limit).map { entities -> entities.map { it.toModel() } }

    fun getUnresolvedCount(): Flow<Int> = dao.getUnresolvedCount()

    suspend fun recordMistake(
        nodeId: String,
        lineId: String,
        side: Side,
        fen: String,
        expectedMove: String,
        userMove: String,
        explanation: String,
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            MistakeItemEntity(
                nodeId = nodeId,
                lineId = lineId,
                side = side.name,
                fen = fen,
                expectedMove = expectedMove,
                userMove = userMove,
                explanation = explanation,
                createdEpochMs = now,
                nextReviewEpochMs = now + 600_000, // review in 10 minutes
            ),
        )
    }

    suspend fun updateReview(item: MistakeItem) {
        dao.update(item.toEntity())
    }

    suspend fun markResolved(id: Long) = dao.markResolved(id)

    private fun MistakeItemEntity.toModel() = MistakeItem(
        id = id, nodeId = nodeId, lineId = lineId,
        side = Side.valueOf(side), fen = fen,
        expectedMove = expectedMove, userMove = userMove,
        explanation = explanation, createdEpochMs = createdEpochMs,
        nextReviewEpochMs = nextReviewEpochMs,
        reviewCount = reviewCount, resolved = resolved,
    )

    private fun MistakeItem.toEntity() = MistakeItemEntity(
        id = id, nodeId = nodeId, lineId = lineId,
        side = side.name, fen = fen,
        expectedMove = expectedMove, userMove = userMove,
        explanation = explanation, createdEpochMs = createdEpochMs,
        nextReviewEpochMs = nextReviewEpochMs,
        reviewCount = reviewCount, resolved = resolved,
    )
}

@Singleton
class ExamResultRepository @Inject constructor(
    private val dao: ExamResultDao,
) {
    fun getAll(): Flow<List<ExamResult>> = dao.getAll().map { entities ->
        entities.map { it.toModel() }
    }

    fun getBySide(side: Side): Flow<List<ExamResult>> =
        dao.getBySide(side.name).map { entities -> entities.map { it.toModel() } }

    suspend fun getBestBySide(side: Side): ExamResult? =
        dao.getBestBySide(side.name)?.toModel()

    suspend fun insert(result: ExamResult): Long = dao.insert(result.toEntity())

    private fun ExamResultEntity.toModel() = ExamResult(
        id = id, side = Side.valueOf(side),
        startedEpochMs = startedEpochMs, finishedEpochMs = finishedEpochMs,
        totalQuestions = totalQuestions, correctAnswers = correctAnswers,
        accuracy = accuracy, avgResponseTimeMs = avgResponseTimeMs,
        branchCoverage = branchCoverage, passed = passed,
    )

    private fun ExamResult.toEntity() = ExamResultEntity(
        id = id, side = side.name,
        startedEpochMs = startedEpochMs, finishedEpochMs = finishedEpochMs,
        totalQuestions = totalQuestions, correctAnswers = correctAnswers,
        accuracy = accuracy, avgResponseTimeMs = avgResponseTimeMs,
        branchCoverage = branchCoverage, passed = passed,
    )
}

@Singleton
class StudySessionRepository @Inject constructor(
    private val dao: StudySessionDao,
) {
    fun getAll(): Flow<List<StudySession>> = dao.getAll().map { entities ->
        entities.map { it.toModel() }
    }

    fun getSince(since: Long): Flow<List<StudySession>> =
        dao.getSince(since).map { entities -> entities.map { it.toModel() } }

    fun getTotalStudyTimeMs(): Flow<Long?> = dao.getTotalStudyTimeMs()

    fun getStudyTimeMsByMode(mode: StudyMode): Flow<Long?> =
        dao.getStudyTimeMsByMode(mode.name)

    /**
     * Calculate current streak: number of consecutive days (ending today or yesterday)
     * with at least one study session.
     */
    suspend fun calculateStreak(): Int {
        val days = dao.getDistinctStudyDays()
        if (days.isEmpty()) return 0

        val todayDay = System.currentTimeMillis() / 86_400_000L
        // Must include today or yesterday to count
        if (days.first() < todayDay - 1) return 0

        var streak = 1
        for (i in 1 until days.size) {
            if (days[i - 1] - days[i] == 1L) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    suspend fun startSession(mode: StudyMode, side: Side? = null): Long {
        return dao.insert(
            StudySessionEntity(
                mode = mode.name,
                side = side?.name,
                startedEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun endSession(id: Long, itemsCompleted: Int, correctCount: Int) {
        val entity = StudySessionEntity(
            id = id,
            mode = "", // will be overwritten by update
            side = null,
            startedEpochMs = 0,
            endedEpochMs = System.currentTimeMillis(),
            itemsCompleted = itemsCompleted,
            correctCount = correctCount,
        )
        dao.update(entity)
    }

    private fun StudySessionEntity.toModel() = StudySession(
        id = id, mode = StudyMode.valueOf(mode),
        side = side?.let { Side.valueOf(it) },
        startedEpochMs = startedEpochMs, endedEpochMs = endedEpochMs,
        itemsCompleted = itemsCompleted, correctCount = correctCount,
    )
}

@Singleton
class BadgeRepository @Inject constructor(
    private val dao: BadgeDao,
) {
    fun getAll(): Flow<List<Badge>> = dao.getAll().map { entities ->
        entities.map { it.toModel() }
    }

    fun getEarned(): Flow<List<Badge>> = dao.getEarned().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun awardBadge(badge: Badge) {
        dao.upsert(badge.toEntity())
    }

    suspend fun seedDefaultBadges() {
        val defaults = listOf(
            BadgeEntity("white_beginner", "White Repertoire Start", "Started learning the White repertoire", "WHITE", null),
            BadgeEntity("black_beginner", "Black Repertoire Start", "Started learning the Black repertoire", "BLACK", null),
            BadgeEntity("white_recall_50", "White Recall 50%", "Recalled 50% of White lines correctly", "WHITE", null),
            BadgeEntity("black_recall_50", "Black Recall 50%", "Recalled 50% of Black lines correctly", "BLACK", null),
            BadgeEntity("white_mastery", "White Mastery", "Mastered the White repertoire (>80% mastery)", "WHITE", null),
            BadgeEntity("black_mastery", "Black Mastery", "Mastered the Black repertoire (>80% mastery)", "BLACK", null),
            BadgeEntity("white_exam_pass", "White Exam Certificate", "Passed the White repertoire exam", "WHITE", null),
            BadgeEntity("black_exam_pass", "Black Exam Certificate", "Passed the Black repertoire exam", "BLACK", null),
            BadgeEntity("streak_7", "Week Warrior", "7-day study streak", null, null),
            BadgeEntity("streak_30", "Monthly Master", "30-day study streak", null, null),
            BadgeEntity("first_mistake_resolved", "First Fix", "Resolved your first mistake", null, null),
            BadgeEntity("woodpecker_10", "Woodpecker Initiate", "Completed 10 Woodpecker cycles", null, null),
        )
        dao.upsertAll(defaults)
    }

    private fun BadgeEntity.toModel() = Badge(
        id = id, title = title, description = description,
        side = side?.let { Side.valueOf(it) },
        earnedEpochMs = earnedEpochMs, iconName = iconName,
    )

    private fun Badge.toEntity() = BadgeEntity(
        id = id, title = title, description = description,
        side = side?.name,
        earnedEpochMs = earnedEpochMs, iconName = iconName,
    )
}
