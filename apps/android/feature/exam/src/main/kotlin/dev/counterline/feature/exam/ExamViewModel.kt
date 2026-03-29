package dev.counterline.feature.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetDrillsUseCase
import dev.counterline.core.domain.RecordDrillAttemptUseCase
import dev.counterline.core.model.Drill
import dev.counterline.core.model.DrillType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamUiState(
    val questions: List<Drill> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val showResult: Boolean = false,
    val score: Int = 0,
    val finished: Boolean = false,
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val getDrills: GetDrillsUseCase,
    private val recordAttempt: RecordDrillAttemptUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState

    init { loadExam() }

    private fun loadExam() {
        viewModelScope.launch {
            // Exam picks multiple-choice and choose-move drills, shuffled
            val drills = getDrills().first()
                .filter { it.options != null && it.options.isNotEmpty() }
                .shuffled()
                .take(20) // 20-question exam
            _uiState.value = ExamUiState(questions = drills)
        }
    }

    fun answer(selected: String) {
        val state = _uiState.value
        if (state.showResult) return
        val q = state.questions.getOrNull(state.currentIndex) ?: return
        val correct = selected == q.correctAnswer

        _uiState.update {
            it.copy(
                selectedAnswer = selected,
                showResult = true,
                score = it.score + if (correct) 1 else 0,
            )
        }

        viewModelScope.launch {
            recordAttempt(q.side?.name ?: "exam", q.id, correct)
        }
    }

    fun next() {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.questions.size) {
            _uiState.update { it.copy(finished = true) }
        } else {
            _uiState.update {
                it.copy(currentIndex = nextIdx, selectedAnswer = null, showResult = false)
            }
        }
    }

    fun restart() { loadExam() }
}
