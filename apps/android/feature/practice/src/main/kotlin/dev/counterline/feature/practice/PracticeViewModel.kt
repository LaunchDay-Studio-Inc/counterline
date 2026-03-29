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

            _uiState.update {
                it.copy(
                    repertoireLine = line,
                    side = line.side,
                    showLineSelector = false,
                    sessionStarted = true,
                    engineReady = true,
                    isUserTurn = line.side == Side.WHITE,
                    currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    moveHistory = emptyList(),
                    moveHistorySan = emptyList(),
                    currentMoveIndex = 0,
                    isInsideRepertoire = true,
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
