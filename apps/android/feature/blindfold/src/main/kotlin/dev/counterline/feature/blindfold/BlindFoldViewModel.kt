package dev.counterline.feature.blindfold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetRepertoireLinesUseCase
import dev.counterline.core.domain.GetReviewQueueUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.ReviewNodeUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import dev.counterline.core.model.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlindFoldUiState(
    val available: Boolean = false,
    val requiredLevel: SkillLevel = SkillLevel.EXPERT_MASTER,
    val lines: List<RepertoireLine> = emptyList(),
    val currentLine: RepertoireLine? = null,
    val currentMoveIndex: Int = 0,
    val userInput: String = "",
    val feedback: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val totalAttempts: Int = 0,
    val sessionComplete: Boolean = false,
    val showMoveList: Boolean = false,
)

@HiltViewModel
class BlindFoldViewModel @Inject constructor(
    private val getLines: GetRepertoireLinesUseCase,
    private val getSettings: GetSettingsUseCase,
    private val reviewNode: ReviewNodeUseCase,
    private val trackSession: TrackStudySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlindFoldUiState())
    val uiState: StateFlow<BlindFoldUiState> = _uiState
    private var sessionId: Long = 0

    init {
        viewModelScope.launch {
            val settings = getSettings().first()
            val available = settings.skillLevel >= SkillLevel.EXPERT_MASTER
            val lines = getLines.forSkillLevel(settings.skillLevel).first()
            _uiState.value = BlindFoldUiState(
                available = available,
                lines = lines,
            )
        }
    }

    fun selectLine(line: RepertoireLine) {
        viewModelScope.launch {
            sessionId = trackSession.start(StudyMode.BLINDFOLD_RECALL, line.side)
        }
        _uiState.update {
            it.copy(
                currentLine = line,
                currentMoveIndex = 0,
                userInput = "",
                feedback = null,
                isCorrect = null,
                score = 0,
                totalAttempts = 0,
                sessionComplete = false,
                showMoveList = false,
            )
        }
    }

    fun submitMove(san: String) {
        val state = _uiState.value
        val line = state.currentLine ?: return
        val move = line.moves.getOrNull(state.currentMoveIndex) ?: return

        val correct = san.trim().equals(move.san, ignoreCase = true)
        val grade = if (correct) ReviewGrade.GOOD else ReviewGrade.FAIL

        viewModelScope.launch {
            reviewNode(
                nodeId = "${line.id}_move_${move.moveNumber}_${if (move.isWhiteMove) "w" else "b"}",
                lineId = line.id,
                side = line.side,
                grade = grade,
            )
        }

        _uiState.update {
            it.copy(
                userInput = san,
                isCorrect = correct,
                feedback = if (correct) "Correct!" else "Expected: ${move.san}",
                score = if (correct) it.score + 1 else it.score,
                totalAttempts = it.totalAttempts + 1,
            )
        }
    }

    fun nextMove() {
        val state = _uiState.value
        val line = state.currentLine ?: return
        val nextIdx = state.currentMoveIndex + 1

        if (nextIdx >= line.moves.size) {
            viewModelScope.launch {
                trackSession.end(sessionId, state.totalAttempts, state.score)
            }
            _uiState.update { it.copy(sessionComplete = true) }
        } else {
            _uiState.update {
                it.copy(
                    currentMoveIndex = nextIdx,
                    userInput = "",
                    feedback = null,
                    isCorrect = null,
                )
            }
        }
    }

    fun toggleMoveList() {
        _uiState.update { it.copy(showMoveList = !it.showMoveList) }
    }

    fun backToLines() {
        _uiState.update {
            it.copy(
                currentLine = null,
                sessionComplete = false,
            )
        }
    }
}
