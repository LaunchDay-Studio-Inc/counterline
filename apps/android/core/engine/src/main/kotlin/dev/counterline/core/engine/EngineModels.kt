package dev.counterline.core.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Raw result from a single engine search */
@Serializable
data class EngineSearchResult(
    val bestmove: String = "",
    val ponder: String = "",
    val depth: Int = 0,
    @SerialName("score_cp") val scoreCp: Int = 0,
    @SerialName("mate_in") val mateIn: Int = 0,
)

/** A single PV line from MultiPV analysis */
data class PvLine(
    val rank: Int,
    val move: String,
    val scoreCp: Int,
    val mateIn: Int = 0,
    val depth: Int = 0,
    val pv: List<String> = emptyList(),
)

/** Full evaluation of a position */
data class PositionEvaluation(
    val fen: String,
    val scoreCp: Int,
    val mateIn: Int = 0,
    val depth: Int,
    val bestMove: String,
    val isApproximate: Boolean = true,
)

/** Result of comparing user move to engine/repertoire move */
data class MoveComparison(
    val userMove: String,
    val repertoireMove: String,
    val engineBestMove: String,
    val userMoveCp: Int? = null,
    val repertoireMoveCp: Int? = null,
    val engineBestCp: Int? = null,
    val centipawnLoss: Int? = null,
)

/** Engine strength profile for practice play */
enum class EngineStrengthProfile(
    val displayName: String,
    val threads: Int,
    val hashMb: Int,
    val depthLimit: Int,
    val movetimeMs: Int,
    val skillLevel: Int,
) {
    TRAINING_EASY(
        displayName = "Training Easy (~1200)",
        threads = 1, hashMb = 16, depthLimit = 8,
        movetimeMs = 200, skillLevel = 3,
    ),
    TRAINING_MEDIUM(
        displayName = "Training Medium (~1600)",
        threads = 1, hashMb = 16, depthLimit = 12,
        movetimeMs = 500, skillLevel = 8,
    ),
    TRAINING_HARD(
        displayName = "Training Hard (~2000)",
        threads = 1, hashMb = 32, depthLimit = 16,
        movetimeMs = 1000, skillLevel = 14,
    ),
    ANALYSIS(
        displayName = "Analysis",
        threads = 1, hashMb = 64, depthLimit = 20,
        movetimeMs = 2000, skillLevel = 20,
    ),
    DEEP_ANALYSIS(
        displayName = "Deep Analysis",
        threads = 1, hashMb = 64, depthLimit = 24,
        movetimeMs = 5000, skillLevel = 20,
    ),
}
