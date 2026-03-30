package dev.counterline.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetRepertoireLinesUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.ReviewNodeUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnUiState(
    val lines: List<RepertoireLine> = emptyList(),
    val selectedLine: RepertoireLine? = null,
    val currentMoveIndex: Int = 0,
    val showExplanation: Boolean = false,
    val lineComplete: Boolean = false,
    val choosingLine: Boolean = true,
    val selectedSide: Side? = null,
    // Phase 2: plan-before-move
    val planPromptActive: Boolean = false,
    val userPlanInput: String = "",
    val planRevealed: Boolean = false,
    // Phase 2: "what changes if you forget" callout
    val showForgetConsequence: Boolean = false,
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val getLines: GetRepertoireLinesUseCase,
    private val getSettings: GetSettingsUseCase,
    private val reviewNode: ReviewNodeUseCase,
    private val trackSession: TrackStudySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState
    private var sessionId: Long = 0

    init {
        viewModelScope.launch {
            val settings = getSettings().first()
            val lines = getLines.forSkillLevel(settings.skillLevel).first()
            _uiState.value = LearnUiState(lines = lines)
        }
    }

    fun filterSide(side: Side?) {
        _uiState.update { it.copy(selectedSide = side) }
        viewModelScope.launch {
            val settings = getSettings().first()
            val all = getLines.forSkillLevel(settings.skillLevel).first()
            val filtered = if (side != null) all.filter { it.side == side } else all
            _uiState.update { it.copy(lines = filtered) }
        }
    }

    fun selectLine(line: RepertoireLine) {
        _uiState.update {
            it.copy(
                selectedLine = line,
                currentMoveIndex = 0,
                showExplanation = false,
                lineComplete = false,
                choosingLine = false,
                planPromptActive = true,
                userPlanInput = "",
                planRevealed = false,
                showForgetConsequence = false,
            )
        }
        viewModelScope.launch {
            sessionId = trackSession.start(StudyMode.LEARN, line.side)
        }
    }

    fun showExplanation() {
        _uiState.update { it.copy(showExplanation = true) }
    }

    fun updatePlanInput(text: String) {
        _uiState.update { it.copy(userPlanInput = text) }
    }

    fun submitPlan() {
        _uiState.update { it.copy(planRevealed = true, planPromptActive = false) }
    }

    fun skipPlan() {
        _uiState.update { it.copy(planPromptActive = false, planRevealed = false) }
    }

    fun toggleForgetConsequence() {
        _uiState.update { it.copy(showForgetConsequence = !it.showForgetConsequence) }
    }

    fun nextMove() {
        val state = _uiState.value
        val line = state.selectedLine ?: return
        val move = line.moves.getOrNull(state.currentMoveIndex)

        if (move != null) {
            viewModelScope.launch {
                reviewNode(
                    nodeId = "${line.id}_move_${move.moveNumber}_${if (move.isWhiteMove) "w" else "b"}",
                    lineId = line.id,
                    side = line.side,
                    grade = ReviewGrade.GOOD,
                )
            }
        }

        val nextIdx = state.currentMoveIndex + 1
        if (nextIdx >= line.moves.size) {
            _uiState.update { it.copy(lineComplete = true) }
            viewModelScope.launch {
                trackSession.end(sessionId, line.moves.size, line.moves.size)
            }
        } else {
            _uiState.update {
                it.copy(
                    currentMoveIndex = nextIdx,
                    showExplanation = false,
                    planPromptActive = true,
                    userPlanInput = "",
                    planRevealed = false,
                    showForgetConsequence = false,
                )
            }
        }
    }

    fun backToLines() {
        _uiState.update {
            it.copy(
                selectedLine = null,
                choosingLine = true,
                currentMoveIndex = 0,
                showExplanation = false,
                lineComplete = false,
            )
        }
    }
}
