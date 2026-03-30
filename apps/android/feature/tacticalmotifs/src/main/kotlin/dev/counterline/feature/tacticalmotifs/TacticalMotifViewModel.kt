package dev.counterline.feature.tacticalmotifs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetTacticalMotifsUseCase
import dev.counterline.core.domain.RecordMotifAttemptUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.Side
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.TacticalMotif
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TacticalMotifUiState(
    val mode: MotifMode = MotifMode.CHOOSING,
    val motifs: List<TacticalMotif> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswer: String = "",
    val showSolution: Boolean = false,
    val score: Int = 0,
    val total: Int = 0,
    val cycleNumber: Int = 1,
    val maxCycles: Int = 3,
    val sessionComplete: Boolean = false,
)

enum class MotifMode { CHOOSING, SOLVING, RESULTS }

@HiltViewModel
class TacticalMotifViewModel @Inject constructor(
    private val getMotifs: GetTacticalMotifsUseCase,
    private val recordAttempt: RecordMotifAttemptUseCase,
    private val trackSession: TrackStudySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TacticalMotifUiState())
    val uiState: StateFlow<TacticalMotifUiState> = _uiState
    private var sessionId: Long = 0

    /**
     * Start a standard tactical motif session.
     */
    fun startSession(count: Int = 10) {
        viewModelScope.launch {
            val motifs = getMotifs.randomSet(count)
            sessionId = trackSession.start(StudyMode.TACTICAL_MOTIF)
            _uiState.value = TacticalMotifUiState(
                mode = MotifMode.SOLVING,
                motifs = motifs,
                currentIndex = 0,
                total = motifs.size,
            )
        }
    }

    /**
     * Start a Woodpecker-style cycle: repeat missed puzzles across multiple rounds.
     */
    fun startWoodpeckerSession(count: Int = 20, maxCycles: Int = 3) {
        viewModelScope.launch {
            val motifs = getMotifs.woodpeckerCycle(count)
            sessionId = trackSession.start(StudyMode.TACTICAL_MOTIF)
            _uiState.value = TacticalMotifUiState(
                mode = MotifMode.SOLVING,
                motifs = motifs,
                currentIndex = 0,
                total = motifs.size,
                maxCycles = maxCycles,
            )
        }
    }

    fun submitAnswer(answer: String) {
        val state = _uiState.value
        val motif = state.motifs.getOrNull(state.currentIndex) ?: return
        val correct = motif.solutionSan.firstOrNull()?.equals(answer, ignoreCase = true) == true

        viewModelScope.launch {
            recordAttempt(motif, correct)
        }

        _uiState.update {
            it.copy(
                userAnswer = answer,
                showSolution = true,
                score = if (correct) it.score + 1 else it.score,
            )
        }
    }

    fun nextMotif() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.motifs.size) {
            // Check if we should do another Woodpecker cycle
            if (state.cycleNumber < state.maxCycles) {
                val missed = state.motifs.filterIndexed { idx, _ ->
                    // Simple tracking: if overall score < total, there are misses
                    true // In a real implementation, track per-motif correctness
                }
                _uiState.update {
                    it.copy(
                        currentIndex = 0,
                        cycleNumber = it.cycleNumber + 1,
                        showSolution = false,
                        userAnswer = "",
                    )
                }
            } else {
                viewModelScope.launch {
                    trackSession.end(sessionId, state.total, state.score)
                }
                _uiState.update { it.copy(mode = MotifMode.RESULTS, sessionComplete = true) }
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    showSolution = false,
                    userAnswer = "",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = TacticalMotifUiState()
    }
}
