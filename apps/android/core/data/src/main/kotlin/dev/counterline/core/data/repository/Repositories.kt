package dev.counterline.core.data.repository

import dev.counterline.core.database.dao.DeviationDao
import dev.counterline.core.database.dao.DrillDao
import dev.counterline.core.database.dao.ModelGameDao
import dev.counterline.core.database.dao.PiecePlacementDao
import dev.counterline.core.database.dao.PlanDao
import dev.counterline.core.database.dao.QuickStartDao
import dev.counterline.core.database.dao.RepertoireDao
import dev.counterline.core.database.dao.ThemeDao
import dev.counterline.core.database.dao.UserProgressDao
import dev.counterline.core.database.entity.UserProgressEntity
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Drill
import dev.counterline.core.model.DrillType
import dev.counterline.core.model.GameAnnotation
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.PiecePlacement
import dev.counterline.core.model.Plan
import dev.counterline.core.model.QuickStart
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.Side
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
                        RepertoireMove(m.moveNumber, m.san, m.purpose, m.isWhiteMove)
                    },
                    memoryHook = entity.memoryHook,
                    memoryHookBreakdown = json.decodeFromString(entity.memoryHookBreakdownJson),
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
                        RepertoireMove(m.moveNumber, m.san, m.purpose, m.isWhiteMove)
                    },
                    memoryHook = entity.memoryHook,
                    memoryHookBreakdown = json.decodeFromString(entity.memoryHookBreakdownJson),
                )
            }
        }
}

@Singleton
class PlanRepository @Inject constructor(private val planDao: PlanDao) {
    fun getAllPlans(): Flow<List<Plan>> = planDao.getAllPlans().map { entities ->
        entities.map { Plan(it.id, Side.valueOf(it.side), it.title, it.description, it.priority) }
    }

    fun getPlansBySide(side: Side): Flow<List<Plan>> = planDao.getPlansBySide(side.name).map { entities ->
        entities.map { Plan(it.id, Side.valueOf(it.side), it.title, it.description, it.priority) }
    }
}

@Singleton
class ThemeRepository @Inject constructor(private val themeDao: ThemeDao) {
    fun getAllThemes(): Flow<List<Theme>> = themeDao.getAllThemes().map { entities ->
        entities.map { Theme(it.id, Side.valueOf(it.side), it.title, it.description, it.occurrenceRate) }
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
            Deviation(it.id, Side.valueOf(it.side), it.deviationName, it.move, it.description, it.response)
        }
    }

    fun getDeviationsBySide(side: Side): Flow<List<Deviation>> =
        deviationDao.getDeviationsBySide(side.name).map { entities ->
            entities.map {
                Deviation(it.id, Side.valueOf(it.side), it.deviationName, it.move, it.description, it.response)
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
