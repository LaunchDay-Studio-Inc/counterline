package dev.counterline.feature.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetDrillsUseCase
import dev.counterline.core.domain.RecordDrillAttemptUseCase
import dev.counterline.core.domain.RecordExamResultUseCase
import dev.counterline.core.model.Drill
import dev.counterline.core.model.ExamResult
import dev.counterline.core.model.Side
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
    val side: Side? = null,
    val startedEpochMs: Long = 0,
    val responseTimes: List<Long> = emptyList(),
    val questionStartMs: Long = 0,
    val examSaved: Boolean = false,
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val getDrills: GetDrillsUseCase,
    private val recordAttempt: RecordDrillAttemptUseCase,
    private val recordExamResult: RecordExamResultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState

    init { loadExam(null) }

    fun loadExam(side: Side?) {
        viewModelScope.launch {
            val allDrills = getDrills().first()
            val filtered = if (side != null) {
                allDrills.filter { it.side == side }
            } else {
                allDrills
            }
            val drills = filtered
                .filter { it.options != null && it.options.isNotEmpty() }
                .shuffled()
                .take(20)
            val now = System.currentTimeMillis()
            _uiState.value = ExamUiState(
                questions = drills,
                side = side,
                startedEpochMs = now,
                questionStartMs = now,
            )
        }
    }

    fun answer(selected: String) {
        val state = _uiState.value
        if (state.showResult) return
        val q = state.questions.getOrNull(state.currentIndex) ?: return
        val correct = selected == q.correctAnswer
        val responseTime = System.currentTimeMillis() - state.questionStartMs

        _uiState.update {
            it.copy(
                selectedAnswer = selected,
                showResult = true,
                score = it.score + if (correct) 1 else 0,
                responseTimes = it.responseTimes + responseTime,
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
            saveExamResult()
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    selectedAnswer = null,
                    showResult = false,
                    questionStartMs = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun saveExamResult() {
        val state = _uiState.value
        if (state.examSaved || state.questions.isEmpty()) return

        viewModelScope.launch {
            val total = state.questions.size
            val correct = state.score
            val accuracy = correct.toFloat() / total
            val avgResponseTime = if (state.responseTimes.isNotEmpty()) {
                state.responseTimes.average().toLong()
            } else 0L
            val now = System.currentTimeMillis()
            val side = state.side ?: Side.WHITE

            recordExamResult(
                ExamResult(
                    side = side,
                    startedEpochMs = state.startedEpochMs,
                    finishedEpochMs = now,
                    totalQuestions = total,
                    correctAnswers = correct,
                    accuracy = accuracy,
                    avgResponseTimeMs = avgResponseTime,
                    branchCoverage = correct.toFloat() / total,
                    passed = accuracy >= 0.7f,
                ),
            )
            _uiState.update { it.copy(examSaved = true) }
        }
    }

    fun restart() { loadExam(_uiState.value.side) }
}
