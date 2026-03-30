package dev.counterline.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetRepertoireLinesUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.ReviewNodeUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.engine.EngineStrengthProfile
import dev.counterline.core.engine.MoveFeedback
import dev.counterline.core.engine.PvLine
import dev.counterline.core.engine.StockfishEngine
import dev.counterline.core.engine.TrainingAssistant
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import dev.counterline.core.model.StudyMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Practice session configuration */
enum class PracticeMode {
    /** Stay inside repertoire lines; reject moves outside the book */
    LINE_LOCK,
    /** Allow deviations; explain how to re-enter or punish */
    DEVIATION,
    /** Start from the tabiya (exit FEN) and play the middlegame */
    PLAY_FROM_TABIYA,
}

data class PracticeUiState(
    val side: Side = Side.WHITE,
    val mode: PracticeMode = PracticeMode.LINE_LOCK,
    val strengthProfile: EngineStrengthProfile = EngineStrengthProfile.TRAINING_MEDIUM,
    val currentFen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val moveHistory: List<String> = emptyList(),
    val moveHistorySan: List<String> = emptyList(),
    val repertoireLine: RepertoireLine? = null,
    val currentMoveIndex: Int = 0,
    val isUserTurn: Boolean = true,
    val isEngineThinking: Boolean = false,
    val lastFeedback: MoveFeedback? = null,
    val isInsideRepertoire: Boolean = true,
    val deviationExplanation: String? = null,
    val sessionStarted: Boolean = false,
    val sessionOver: Boolean = false,
    val movesPlayed: Int = 0,
    val correctMoves: Int = 0,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
    val availableLines: List<RepertoireLine> = emptyList(),
    val showLineSelector: Boolean = true,
    val engineReady: Boolean = false,
    // Phase 0 — elite analysis pane
    val showAnalysisPane: Boolean = false,
    val analysisEval: String? = null,
    val analysisBestMove: String? = null,
    val analysisDepth: Int = 0,
    val analysisTopMoves: List<MoveComparisonEntry> = emptyList(),
    // Phase 0 — explain last move
    val lastMoveExplanation: String? = null,
    // Phase 0 — compare your move
    val moveComparison: MoveComparisonResult? = null,
)

/** A single engine-ranked move for the analysis pane */
data class MoveComparisonEntry(
    val rank: Int,
    val move: String,
    val scoreCp: Int,
    val isRepertoireMove: Boolean = false,
    val isUserMove: Boolean = false,
)

/** Result of comparing user move vs repertoire move vs engine best move */
data class MoveComparisonResult(
    val userMove: String,
    val userMoveCp: Int?,
    val repertoireMove: String,
    val repertoireMoveCp: Int?,
    val engineBestMove: String,
    val engineBestCp: Int?,
    val verdict: String,
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val engine: StockfishEngine,
    private val trainingAssistant: TrainingAssistant,
    private val getRepertoireLines: GetRepertoireLinesUseCase,
    private val getSettings: GetSettingsUseCase,
    private val reviewNode: ReviewNodeUseCase,
    private val trackSession: TrackStudySessionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState
    private var sessionId: Long = 0
    private var engineJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = getSettings().first()
            val lines = getRepertoireLines().first()
            _uiState.update {
                it.copy(
                    skillLevel = settings.skillLevel,
                    availableLines = lines,
                )
            }
        }
    }

    /** Select a repertoire line and start the practice session. */
    fun selectLine(line: RepertoireLine) {
        viewModelScope.launch {
            // Start engine
            engine.startSession()
            engine.setEngineStrengthProfile(_uiState.value.strengthProfile)

            sessionId = trackSession.start(StudyMode.RECALL, line.side)

            val startFen = if (_uiState.value.mode == PracticeMode.PLAY_FROM_TABIYA) {
                line.exitFen
            } else {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            }

            val startIndex = if (_uiState.value.mode == PracticeMode.PLAY_FROM_TABIYA) {
                line.moves.size // skip all repertoire moves — start from the tabiya
            } else {
                0
            }

            _uiState.update {
                it.copy(
                    repertoireLine = line,
                    side = line.side,
                    showLineSelector = false,
                    sessionStarted = true,
                    engineReady = true,
                    isUserTurn = line.side == Side.WHITE,
                    currentFen = startFen,
                    moveHistory = emptyList(),
                    moveHistorySan = emptyList(),
                    currentMoveIndex = startIndex,
                    isInsideRepertoire = _uiState.value.mode != PracticeMode.PLAY_FROM_TABIYA,
                )
            }

            // If engine moves first (user is Black), make engine's move
            if (line.side == Side.BLACK) {
                makeEngineMove()
            }
        }
    }

    /** User makes a move (in UCI notation, e.g. "e2e4"). */
    fun userMove(uciMove: String) {
        val state = _uiState.value
        if (!state.isUserTurn || state.isEngineThinking || state.sessionOver) return
        val line = state.repertoireLine ?: return

        viewModelScope.launch {
            val expectedMove = getExpectedRepertoireMove(state)

            if (expectedMove != null) {
                // We're inside the repertoire — compare the move
                val feedback = trainingAssistant.compareMove(
                    userMove = uciMove,
                    repertoireMove = expectedMove,
                    fen = state.currentFen,
                    skillLevel = state.skillLevel,
                )

                val isCorrect = feedback.isCorrect

                // Record for SRS
                reviewNode(
                    nodeId = "${line.id}_move_${state.currentMoveIndex}",
                    lineId = line.id,
                    side = line.side,
                    grade = if (isCorrect) ReviewGrade.GOOD else ReviewGrade.FAIL,
                    fen = state.currentFen,
                    expectedMove = expectedMove.san,
                    userMove = uciMove,
                    explanation = feedback.explanation,
                )

                if (state.mode == PracticeMode.LINE_LOCK && !isCorrect) {
                    // Line lock: show feedback but don't apply the wrong move
                    _uiState.update {
                        it.copy(
                            lastFeedback = feedback,
                            movesPlayed = it.movesPlayed + 1,
                        )
                    }
                    return@launch
                }

                // Apply the move
                val newMoves = state.moveHistory + uciMove
                val newSan = state.moveHistorySan + (if (isCorrect) expectedMove.san else uciMove)
                val nextIndex = state.currentMoveIndex + 1

                // Check if we've finished the repertoire line
                val lineComplete = nextIndex >= line.moves.size

                _uiState.update {
                    it.copy(
                        moveHistory = newMoves,
                        moveHistorySan = newSan,
                        currentMoveIndex = nextIndex,
                        lastFeedback = feedback,
                        isUserTurn = false,
                        movesPlayed = it.movesPlayed + 1,
                        correctMoves = it.correctMoves + if (isCorrect) 1 else 0,
                        isInsideRepertoire = isCorrect && state.isInsideRepertoire,
                        sessionOver = lineComplete,
                    )
                }

                if (!lineComplete) {
                    makeEngineMove()
                } else {
                    endSession()
                }
            } else {
                // Outside repertoire — deviation mode
                handleDeviation(uciMove)
            }
        }
    }

    /** Set the practice mode (line lock or deviation). */
    fun setPracticeMode(mode: PracticeMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    /** Set the engine strength. */
    fun setStrength(profile: EngineStrengthProfile) {
        _uiState.update { it.copy(strengthProfile = profile) }
        viewModelScope.launch {
            engine.setEngineStrengthProfile(profile)
        }
    }

    /** Dismiss the current feedback message. */
    fun dismissFeedback() {
        _uiState.update { it.copy(lastFeedback = null, deviationExplanation = null) }
    }

    /** End the session and clean up. */
    fun endPracticeSession() {
        viewModelScope.launch {
            endSession()
        }
    }

    // ────────────────────────────────────────────────────────
    // Phase 0 — Elite Analysis Pane
    // ────────────────────────────────────────────────────────

    /** Toggle the analysis pane and trigger a deep evaluation. */
    fun toggleAnalysisPane() {
        val show = !_uiState.value.showAnalysisPane
        _uiState.update { it.copy(showAnalysisPane = show) }
        if (show) refreshAnalysis()
    }

    /** Run a deep multi-PV analysis on the current position. */
    fun refreshAnalysis() {
        val state = _uiState.value
        if (!state.engineReady) return
        viewModelScope.launch {
            try {
                engine.setEngineStrengthProfile(EngineStrengthProfile.DEEP_ANALYSIS)
                val topMoves = engine.getTopMoves(state.currentFen, n = 4, depth = 22)
                val repertoireMove = getExpectedRepertoireMove(state)?.san

                val entries = topMoves.mapIndexed { idx, pv ->
                    MoveComparisonEntry(
                        rank = idx + 1,
                        move = pv.move,
                        scoreCp = pv.scoreCp,
                        isRepertoireMove = pv.move.equals(repertoireMove, ignoreCase = true),
                    )
                }

                val best = topMoves.firstOrNull()
                _uiState.update {
                    it.copy(
                        analysisTopMoves = entries,
                        analysisEval = best?.let { formatCp(it.scoreCp) },
                        analysisBestMove = best?.move,
                        analysisDepth = best?.depth ?: 0,
                    )
                }
                // restore practice strength
                engine.setEngineStrengthProfile(state.strengthProfile)
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(analysisEval = "N/A", analysisTopMoves = emptyList())
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Phase 0 — Compare Your Move vs Repertoire vs Engine
    // ────────────────────────────────────────────────────────

    /** After the user plays a move, compare it against both repertoire and engine best. */
    fun compareLastMove() {
        val state = _uiState.value
        val userMove = state.moveHistorySan.lastOrNull() ?: return
        val repertoireMove = getExpectedRepertoireMove(
            state.copy(currentMoveIndex = state.currentMoveIndex - 1),
        )?.san ?: return

        viewModelScope.launch {
            try {
                engine.setEngineStrengthProfile(EngineStrengthProfile.ANALYSIS)
                val eval = engine.evaluateFen(state.currentFen, depth = 18)

                val result = MoveComparisonResult(
                    userMove = userMove,
                    userMoveCp = null, // would need pre-move position
                    repertoireMove = repertoireMove,
                    repertoireMoveCp = null,
                    engineBestMove = eval.bestMove,
                    engineBestCp = eval.scoreCp,
                    verdict = buildCompareVerdict(userMove, repertoireMove, eval.bestMove),
                )
                _uiState.update { it.copy(moveComparison = result) }
                engine.setEngineStrengthProfile(state.strengthProfile)
            } catch (_: Exception) {
                // silently ignore
            }
        }
    }

    /** Dismiss the move comparison overlay. */
    fun dismissComparison() {
        _uiState.update { it.copy(moveComparison = null) }
    }

    // ────────────────────────────────────────────────────────
    // Phase 0 — Explain Last Move
    // ────────────────────────────────────────────────────────

    /** Explain the last move played (by engine or user). */
    fun explainLastMove() {
        val state = _uiState.value
        val lastSan = state.moveHistorySan.lastOrNull() ?: return
        val line = state.repertoireLine ?: return

        // Look up the move in the repertoire for a purpose annotation
        val idx = state.currentMoveIndex - 1
        val repMove = if (idx >= 0 && idx < line.moves.size) line.moves[idx] else null

        val explanation = if (repMove != null && repMove.san.equals(lastSan, ignoreCase = true)) {
            buildString {
                append("${repMove.san}: ")
                if (repMove.whyThisMove.isNotEmpty()) {
                    append(repMove.whyThisMove)
                } else if (repMove.purpose.isNotEmpty()) {
                    append(repMove.purpose)
                } else {
                    append("This is the repertoire's recommended move at this stage.")
                }
                if (repMove.keyPlanCallout.isNotEmpty()) {
                    append("\n\nKey plan: ${repMove.keyPlanCallout}")
                }
            }
        } else {
            // Outside repertoire — provide generic engine context
            "$lastSan — This move is outside the repertoire. " +
                "Consult the analysis pane for engine evaluation."
        }

        _uiState.update { it.copy(lastMoveExplanation = explanation) }
    }

    /** Dismiss the last-move explanation. */
    fun dismissExplanation() {
        _uiState.update { it.copy(lastMoveExplanation = null) }
    }

    private fun buildCompareVerdict(
        userMove: String,
        repertoireMove: String,
        engineBest: String,
    ): String {
        val userIsRepertoire = userMove.equals(repertoireMove, ignoreCase = true)
        val userIsEngine = userMove.equals(engineBest, ignoreCase = true)
        val repertoireIsEngine = repertoireMove.equals(engineBest, ignoreCase = true)

        return when {
            userIsRepertoire && userIsEngine ->
                "Your move matches both the repertoire and the engine's top choice."
            userIsRepertoire ->
                "Your move matches the repertoire. The engine slightly prefers $engineBest at this depth."
            userIsEngine ->
                "Your move matches the engine's top pick, but the repertoire recommends $repertoireMove."
            else ->
                "Your move differs from both the repertoire ($repertoireMove) and the engine ($engineBest)."
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineJob?.cancel()
        viewModelScope.launch {
            engine.cancelAnalysis()
            engine.stopSession()
        }
    }

    private suspend fun makeEngineMove() {
        val state = _uiState.value
        val line = state.repertoireLine ?: return

        _uiState.update { it.copy(isEngineThinking = true) }

        val engineMove: String
        val isInsideRepertoire: Boolean

        // If we're still inside the repertoire, use the repertoire move
        val repertoireMove = getExpectedRepertoireMove(state)
        if (repertoireMove != null && state.isInsideRepertoire) {
            engineMove = repertoireMove.san
            isInsideRepertoire = true
        } else {
            // Outside repertoire — let the engine play freely
            engineMove = engine.getBestMove(
                fen = state.currentFen,
                movetimeMs = state.strengthProfile.movetimeMs,
            )
            isInsideRepertoire = false
        }

        val newMoves = state.moveHistory + engineMove
        val newSan = state.moveHistorySan + engineMove
        val nextIndex = state.currentMoveIndex + 1

        _uiState.update {
            it.copy(
                moveHistory = newMoves,
                moveHistorySan = newSan,
                currentMoveIndex = nextIndex,
                isUserTurn = true,
                isEngineThinking = false,
                isInsideRepertoire = isInsideRepertoire,
            )
        }
    }

    private suspend fun handleDeviation(uciMove: String) {
        val state = _uiState.value

        // Evaluate the deviation
        val explanation = try {
            val eval = engine.evaluateFen(state.currentFen)
            "You've left the repertoire. Engine evaluation: ~${formatCp(eval.scoreCp)}. " +
                "The repertoire line keeps the advantage with a clear plan."
        } catch (_: Exception) {
            "You've left the repertoire. The repertoire line was chosen for a clear strategic plan."
        }

        val newMoves = state.moveHistory + uciMove
        val newSan = state.moveHistorySan + uciMove

        _uiState.update {
            it.copy(
                moveHistory = newMoves,
                moveHistorySan = newSan,
                isInsideRepertoire = false,
                deviationExplanation = explanation,
                isUserTurn = false,
                movesPlayed = it.movesPlayed + 1,
            )
        }

        // Engine responds to deviation freely
        makeEngineMove()
    }

    private fun getExpectedRepertoireMove(state: PracticeUiState): RepertoireMove? {
        val line = state.repertoireLine ?: return null
        val idx = state.currentMoveIndex
        return if (idx < line.moves.size) line.moves[idx] else null
    }

    private suspend fun endSession() {
        val state = _uiState.value
        _uiState.update { it.copy(sessionOver = true) }
        trackSession.end(sessionId, state.movesPlayed, state.correctMoves)
        engine.stopSession()
    }

    private fun formatCp(cp: Int): String {
        val sign = if (cp >= 0) "+" else ""
        return "$sign${String.format("%.2f", cp / 100.0)}"
    }
}
