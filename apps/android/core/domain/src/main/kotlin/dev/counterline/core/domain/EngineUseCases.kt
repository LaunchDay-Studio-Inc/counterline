package dev.counterline.core.domain

import dev.counterline.core.engine.PositionEvaluation
import dev.counterline.core.engine.StockfishEngine
import dev.counterline.core.engine.TrainingAssistant
import dev.counterline.core.engine.WhyNotAnalysis
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.MistakeItem
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.Plan
import dev.counterline.core.model.SkillLevel
import javax.inject.Inject

/**
 * Enriches existing content (plans, deviations, model games, mistakes)
 * with on-demand engine analysis. The engine supplements — never replaces —
 * the curated course content.
 */

/** Enrich a plan with engine evaluation of the exit position */
data class EnrichedPlan(
    val plan: Plan,
    val exitEvaluation: PositionEvaluation? = null,
)

/** Enrich a deviation with engine analysis of the response */
data class EnrichedDeviation(
    val deviation: Deviation,
    val deviationEval: PositionEvaluation? = null,
    val responseEval: PositionEvaluation? = null,
    val centipawnSwing: Int? = null,
)

/** Enrich a model game position with engine evaluation */
data class EnrichedGamePosition(
    val moveNumber: Int,
    val fen: String,
    val annotation: String,
    val evaluation: PositionEvaluation? = null,
)

/** Enrich a mistake with engine analysis */
data class EnrichedMistake(
    val mistake: MistakeItem,
    val positionEval: PositionEvaluation? = null,
    val whyNotAnalysis: WhyNotAnalysis? = null,
)

class AnalyzePlanUseCase @Inject constructor(
    private val engine: StockfishEngine,
) {
    /**
     * Evaluate the exit position for a plan to show how favorable it is.
     * Only runs if the engine is active and the user is at sufficient skill level.
     */
    suspend fun enrich(
        plan: Plan,
        exitFen: String,
        skillLevel: SkillLevel,
    ): EnrichedPlan {
        if (skillLevel < SkillLevel.EXPERT_MASTER || !engine.isReady()) {
            return EnrichedPlan(plan = plan)
        }
        return try {
            val eval = engine.evaluateFen(exitFen, depth = 16)
            EnrichedPlan(plan = plan, exitEvaluation = eval)
        } catch (_: Exception) {
            EnrichedPlan(plan = plan)
        }
    }
}

class AnalyzeDeviationUseCase @Inject constructor(
    private val engine: StockfishEngine,
) {
    /**
     * Evaluate a deviation and its repertoire response to show
     * how much the deviation costs the opponent.
     */
    suspend fun enrich(
        deviation: Deviation,
        deviationFen: String,
        responseFen: String,
        skillLevel: SkillLevel,
    ): EnrichedDeviation {
        if (skillLevel < SkillLevel.EXPERT_MASTER || !engine.isReady()) {
            return EnrichedDeviation(deviation = deviation)
        }
        return try {
            val devEval = engine.evaluateFen(deviationFen, depth = 14)
            val respEval = engine.evaluateFen(responseFen, depth = 14)
            val swing = respEval.scoreCp - devEval.scoreCp
            EnrichedDeviation(
                deviation = deviation,
                deviationEval = devEval,
                responseEval = respEval,
                centipawnSwing = swing,
            )
        } catch (_: Exception) {
            EnrichedDeviation(deviation = deviation)
        }
    }
}

class AnalyzeModelGameUseCase @Inject constructor(
    private val engine: StockfishEngine,
) {
    /**
     * Analyze key positions in a model game to show evaluation progression.
     * Only analyzes positions at key moments, not every move.
     */
    suspend fun analyzeKeyPositions(
        game: ModelGame,
        fens: List<Pair<Int, String>>,
        skillLevel: SkillLevel,
    ): List<EnrichedGamePosition> {
        if (skillLevel < SkillLevel.ADVANCED_CLUB || !engine.isReady()) {
            return fens.map { (moveNum, fen) ->
                val annotation = game.annotations
                    .find { it.moveNumber == moveNum }?.comment ?: ""
                EnrichedGamePosition(
                    moveNumber = moveNum,
                    fen = fen,
                    annotation = annotation,
                )
            }
        }

        return fens.map { (moveNum, fen) ->
            val annotation = game.annotations
                .find { it.moveNumber == moveNum }?.comment ?: ""
            val eval = try {
                engine.evaluateFen(fen, depth = 14)
            } catch (_: Exception) {
                null
            }
            EnrichedGamePosition(
                moveNumber = moveNum,
                fen = fen,
                annotation = annotation,
                evaluation = eval,
            )
        }
    }
}

class AnalyzeMistakeUseCase @Inject constructor(
    private val engine: StockfishEngine,
    private val trainingAssistant: TrainingAssistant,
) {
    /**
     * Provide deeper analysis of why a mistake was wrong,
     * including engine evaluation and "why not" explanation.
     */
    suspend fun enrich(
        mistake: MistakeItem,
        skillLevel: SkillLevel,
    ): EnrichedMistake {
        if (skillLevel < SkillLevel.EXPERT_MASTER || !engine.isReady()) {
            return EnrichedMistake(mistake = mistake)
        }
        val eval = try {
            engine.evaluateFen(mistake.fen, depth = 16)
        } catch (_: Exception) {
            null
        }
        val whyNot = try {
            trainingAssistant.analyzeWhyNot(
                move = mistake.userMove,
                fen = mistake.fen,
                repertoireMove = mistake.expectedMove,
            )
        } catch (_: Exception) {
            null
        }
        return EnrichedMistake(
            mistake = mistake,
            positionEval = eval,
            whyNotAnalysis = whyNot,
        )
    }
}
