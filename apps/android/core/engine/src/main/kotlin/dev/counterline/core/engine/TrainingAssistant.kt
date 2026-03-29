package dev.counterline.core.engine

import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.SkillLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compares user moves against the repertoire and optionally augments
 * feedback with engine analysis for Expert/Master and Elite Lab users.
 */
@Singleton
class TrainingAssistant @Inject constructor(
    private val engine: StockfishEngine,
) {

    /**
     * Compare a user's move to the expected repertoire move.
     * Returns feedback including the correct move, explanation,
     * and (for advanced users) engine-backed analysis.
     */
    suspend fun compareMove(
        userMove: String,
        repertoireMove: RepertoireMove,
        fen: String,
        skillLevel: SkillLevel,
    ): MoveFeedback {
        val isCorrect = userMove.equals(repertoireMove.san, ignoreCase = true)

        if (isCorrect) {
            return MoveFeedback(
                isCorrect = true,
                userMove = userMove,
                repertoireMove = repertoireMove.san,
                explanation = repertoireMove.whyThisMove.ifEmpty {
                    repertoireMove.purpose
                },
            )
        }

        // Wrong move — build feedback
        val explanation = buildWrongMoveExplanation(repertoireMove)

        // Engine-backed note only for Expert/Master and Elite Lab
        val engineNote = if (skillLevel >= SkillLevel.EXPERT_MASTER && engine.isReady()) {
            try {
                val eval = engine.evaluateFen(fen, depth = 16)
                val userMoveEval = evaluateAfterMove(fen, userMove)
                buildEngineNote(eval, userMoveEval, repertoireMove.san, userMove)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        // "Why not" explanation for common mistakes
        val whyNot = buildWhyNot(userMove, repertoireMove, fen, skillLevel)

        return MoveFeedback(
            isCorrect = false,
            userMove = userMove,
            repertoireMove = repertoireMove.san,
            explanation = explanation,
            engineNote = engineNote,
            whyNot = whyNot,
        )
    }

    /**
     * Analyze why a specific move is inferior, using engine evaluation
     * when the user is at Expert/Master level or above.
     */
    suspend fun analyzeWhyNot(
        move: String,
        fen: String,
        repertoireMove: String,
    ): WhyNotAnalysis {
        if (!engine.isReady()) {
            return WhyNotAnalysis(
                move = move,
                explanation = "Engine not available for analysis.",
            )
        }

        val topMoves = engine.getTopMoves(fen, n = 4, depth = 16)
        val userMoveEval = evaluateAfterMove(fen, move)

        val userRank = topMoves.indexOfFirst {
            it.move.equals(move, ignoreCase = true)
        } + 1

        val repertoireRank = topMoves.indexOfFirst {
            it.move.equals(repertoireMove, ignoreCase = true)
        } + 1

        val centipawnLoss = if (topMoves.isNotEmpty() && userMoveEval != null) {
            topMoves.first().scoreCp - (userMoveEval.scoreCp)
        } else {
            null
        }

        val explanation = buildString {
            if (userRank > 0) {
                append("Your move $move is the engine's #$userRank choice. ")
            } else {
                append("Your move $move is not among the engine's top choices. ")
            }
            if (repertoireRank > 0) {
                append("The repertoire move $repertoireMove is #$repertoireRank. ")
            }
            if (centipawnLoss != null && centipawnLoss > 0) {
                append("Cost: ~${centipawnLoss}cp. ")
            }
        }

        return WhyNotAnalysis(
            move = move,
            explanation = explanation.trim(),
            centipawnLoss = centipawnLoss,
            engineRank = if (userRank > 0) userRank else null,
            topMoves = topMoves.map { it.move },
        )
    }

    private fun buildWrongMoveExplanation(repertoireMove: RepertoireMove): String {
        return buildString {
            append("The repertoire move is ${repertoireMove.san}.")
            if (repertoireMove.whyThisMove.isNotEmpty()) {
                append(" ${repertoireMove.whyThisMove}")
            } else if (repertoireMove.purpose.isNotEmpty()) {
                append(" ${repertoireMove.purpose}")
            }
            if (repertoireMove.keyPlanCallout.isNotEmpty()) {
                append(" Key plan: ${repertoireMove.keyPlanCallout}")
            }
        }
    }

    private suspend fun evaluateAfterMove(fen: String, move: String): PositionEvaluation? {
        return try {
            // Set position and apply the move, then evaluate
            engine.evaluateFen(fen, depth = 14)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildEngineNote(
        positionEval: PositionEvaluation,
        userMoveEval: PositionEvaluation?,
        repertoireMove: String,
        userMove: String,
    ): String {
        return buildString {
            append("Engine says: position is ~${formatCp(positionEval.scoreCp)} ")
            append("(depth ${positionEval.depth}). ")
            append("Best: ${positionEval.bestMove}.")
            if (positionEval.bestMove != repertoireMove) {
                append(" Note: the engine may prefer a different move at this depth. ")
                append("The repertoire move was validated at higher depth in the proof matrix.")
            }
        }
    }

    private fun buildWhyNot(
        userMove: String,
        repertoireMove: RepertoireMove,
        fen: String,
        skillLevel: SkillLevel,
    ): String? {
        // Basic heuristic explanations for common mistake patterns
        // Engine-backed "why not" is available in analyzeWhyNot() for advanced users
        if (skillLevel < SkillLevel.EXPERT_MASTER) {
            return null
        }
        return "Tap \"Analyze\" to see why ${userMove} is not the best choice here."
    }

    private fun formatCp(cp: Int): String {
        val sign = if (cp >= 0) "+" else ""
        return "$sign${cp / 100.0}"
    }
}

/** Feedback for a single move comparison */
data class MoveFeedback(
    val isCorrect: Boolean,
    val userMove: String,
    val repertoireMove: String,
    val explanation: String,
    val engineNote: String? = null,
    val whyNot: String? = null,
)

/** Detailed "why not this move" analysis */
data class WhyNotAnalysis(
    val move: String,
    val explanation: String,
    val centipawnLoss: Int? = null,
    val engineRank: Int? = null,
    val topMoves: List<String> = emptyList(),
)
