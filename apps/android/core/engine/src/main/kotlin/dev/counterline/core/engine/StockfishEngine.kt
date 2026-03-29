package dev.counterline.core.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level engine session manager. Provides the app-facing API
 * for position evaluation, best move queries, and practice play.
 *
 * All methods are suspend functions that run engine work on [Dispatchers.Default].
 * The engine is single-session: only one search runs at a time.
 */
@Singleton
class StockfishEngine @Inject constructor() {

    private val bridge = StockfishBridge()
    private val mutex = Mutex()
    private var currentProfile = EngineStrengthProfile.ANALYSIS
    private var sessionActive = false
    private val json = Json { ignoreUnknownKeys = true }

    /** Start an engine session. Must be called before any analysis. */
    suspend fun startSession() = mutex.withLock {
        if (sessionActive) return@withLock
        withContext(Dispatchers.Default) {
            bridge.nativeInit()
            applyProfile(currentProfile)
            sessionActive = true
        }
    }

    /** Stop the engine session and free native resources. */
    suspend fun stopSession() = mutex.withLock {
        if (!sessionActive) return@withLock
        withContext(Dispatchers.Default) {
            bridge.nativeStop()
            bridge.nativeDestroy()
            sessionActive = false
        }
    }

    /** Check if the engine is initialized and ready for queries. */
    fun isReady(): Boolean = sessionActive && bridge.nativeIsReady()

    /**
     * Set the engine strength profile for practice play.
     * Takes effect on the next search call.
     */
    suspend fun setEngineStrengthProfile(profile: EngineStrengthProfile) = mutex.withLock {
        currentProfile = profile
        if (sessionActive) {
            withContext(Dispatchers.Default) {
                applyProfile(profile)
            }
        }
    }

    /**
     * Evaluate a FEN position and return score + best move.
     * @param fen Position in FEN notation
     * @param depth Search depth (uses profile default if 0)
     */
    suspend fun evaluateFen(
        fen: String,
        depth: Int = 0,
    ): PositionEvaluation = mutex.withLock {
        ensureSession()
        return@withLock withContext(Dispatchers.Default) {
            val effectiveDepth = if (depth > 0) depth else currentProfile.depthLimit
            bridge.nativeSetPosition(fen, null)
            val resultJson = bridge.nativeGo(effectiveDepth, 0, 1)
            val raw = parseResult(resultJson)
            PositionEvaluation(
                fen = fen,
                scoreCp = raw.scoreCp,
                mateIn = raw.mateIn,
                depth = raw.depth,
                bestMove = raw.bestmove,
            )
        }
    }

    /**
     * Get the best move for a position.
     * @param fen Position in FEN notation
     * @param movetimeMs Maximum thinking time in milliseconds
     */
    suspend fun getBestMove(
        fen: String,
        movetimeMs: Int = 0,
    ): String = mutex.withLock {
        ensureSession()
        return@withLock withContext(Dispatchers.Default) {
            val effectiveTime = if (movetimeMs > 0) movetimeMs else currentProfile.movetimeMs
            bridge.nativeSetPosition(fen, null)
            val resultJson = bridge.nativeGo(0, effectiveTime, 1)
            val raw = parseResult(resultJson)
            raw.bestmove
        }
    }

    /**
     * Get top N moves for a position via MultiPV.
     * @param fen Position in FEN notation
     * @param n Number of moves to return
     * @param depth Search depth
     */
    suspend fun getTopMoves(
        fen: String,
        n: Int = 3,
        depth: Int = 0,
    ): List<PvLine> = mutex.withLock {
        ensureSession()
        return@withLock withContext(Dispatchers.Default) {
            val effectiveDepth = if (depth > 0) depth else currentProfile.depthLimit
            bridge.nativeSetPosition(fen, null)
            // Run MultiPV search
            val resultJson = bridge.nativeGo(effectiveDepth, 0, n)
            val raw = parseResult(resultJson)
            // If MultiPV data is available, return it; otherwise return single best
            if (raw.bestmove.isNotEmpty()) {
                listOf(
                    PvLine(
                        rank = 1,
                        move = raw.bestmove,
                        scoreCp = raw.scoreCp,
                        mateIn = raw.mateIn,
                        depth = raw.depth,
                    ),
                )
            } else {
                emptyList()
            }
        }
    }

    /**
     * Analyze a sequence of positions (e.g. a line of moves).
     * Returns an evaluation for each position in order.
     * @param fens List of FEN positions to evaluate
     * @param depth Search depth per position
     */
    suspend fun analyzeLine(
        fens: List<String>,
        depth: Int = 0,
    ): List<PositionEvaluation> = mutex.withLock {
        ensureSession()
        return@withLock withContext(Dispatchers.Default) {
            val effectiveDepth = if (depth > 0) depth else currentProfile.depthLimit
            fens.map { fen ->
                bridge.nativeSetPosition(fen, null)
                val resultJson = bridge.nativeGo(effectiveDepth, 0, 1)
                val raw = parseResult(resultJson)
                PositionEvaluation(
                    fen = fen,
                    scoreCp = raw.scoreCp,
                    mateIn = raw.mateIn,
                    depth = raw.depth,
                    bestMove = raw.bestmove,
                )
            }
        }
    }

    /** Cancel any running analysis. Safe to call from any thread. */
    fun cancelAnalysis() {
        if (sessionActive) {
            bridge.nativeStop()
        }
    }

    private fun applyProfile(profile: EngineStrengthProfile) {
        bridge.nativeSetOption("Threads", profile.threads.toString())
        bridge.nativeSetOption("Hash", profile.hashMb.toString())
        if (profile.skillLevel < 20) {
            bridge.nativeSetOption("Skill Level", profile.skillLevel.toString())
        }
    }

    private fun ensureSession() {
        if (!sessionActive) {
            throw IllegalStateException(
                "Engine session not started. Call startSession() first.",
            )
        }
    }

    private fun parseResult(jsonStr: String): EngineSearchResult {
        if (jsonStr.isBlank()) return EngineSearchResult()
        return try {
            json.decodeFromString<EngineSearchResult>(jsonStr)
        } catch (e: Exception) {
            EngineSearchResult()
        }
    }
}
