package dev.counterline.feature.drill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetDrillsUseCase
import dev.counterline.core.domain.RecordDrillAttemptUseCase
import dev.counterline.core.model.Drill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DrillUiState(
    val drills: List<Drill> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val showResult: Boolean = false,
    val correctCount: Int = 0,
    val totalAnswered: Int = 0,
    val sessionComplete: Boolean = false,
)

@HiltViewModel
class DrillViewModel @Inject constructor(
    private val getDrills: GetDrillsUseCase,
    private val recordAttempt: RecordDrillAttemptUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrillUiState())
    val uiState: StateFlow<DrillUiState> = _uiState

    init {
        viewModelScope.launch {
            val drills = getDrills().first().shuffled()
            _uiState.update { it.copy(drills = drills) }
        }
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value
        if (state.showResult) return
        val drill = state.drills.getOrNull(state.currentIndex) ?: return
        val correct = answer.equals(drill.correctAnswer, ignoreCase = true)

        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                showResult = true,
                correctCount = it.correctCount + if (correct) 1 else 0,
                totalAnswered = it.totalAnswered + 1,
            )
        }

        viewModelScope.launch {
            recordAttempt(
                lineId = drill.side?.name ?: "general",
                drillId = drill.id,
                correct = correct,
            )
        }
    }

    fun next() {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.drills.size) {
            _uiState.update { it.copy(sessionComplete = true) }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    selectedAnswer = null,
                    showResult = false,
                )
            }
        }
    }

    fun restart() {
        viewModelScope.launch {
            val drills = getDrills().first().shuffled()
            _uiState.value = DrillUiState(drills = drills)
        }
    }
}
