package dev.counterline.feature.quick5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetDrillsUseCase
import dev.counterline.core.domain.GetMistakesUseCase
import dev.counterline.core.domain.GetReviewQueueUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.RecordDrillAttemptUseCase
import dev.counterline.core.domain.ReviewNodeUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.Drill
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.Side
import dev.counterline.core.model.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Quick5UiState(
    val drills: List<Drill> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val showResult: Boolean = false,
    val correctCount: Int = 0,
    val totalAnswered: Int = 0,
    val sessionComplete: Boolean = false,
    val elapsedMs: Long = 0,
    val startedMs: Long = 0,
)

@HiltViewModel
class Quick5ViewModel @Inject constructor(
    private val getDrills: GetDrillsUseCase,
    private val getSettings: GetSettingsUseCase,
    private val getMistakes: GetMistakesUseCase,
    private val recordAttempt: RecordDrillAttemptUseCase,
    private val reviewNode: ReviewNodeUseCase,
    private val trackSession: TrackStudySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(Quick5UiState())
    val uiState: StateFlow<Quick5UiState> = _uiState
    private var sessionId: Long = 0

    companion object {
        private const val QUICK_5_COUNT = 5
    }

    init { loadSession() }

    private fun loadSession() {
        viewModelScope.launch {
            sessionId = trackSession.start(StudyMode.QUICK_5)
            val settings = getSettings().first()
            val allDrills = getDrills.forSkillLevel(settings.skillLevel).first()

            // High-value prioritization: mix overdue mistakes with random drills
            val unresolvedMistakes = getMistakes.unresolved().first()
            val mistakeDrills = unresolvedMistakes
                .take(2) // up to 2 mistake-based items
                .map { mistake ->
                    Drill(
                        id = "mistake_${mistake.id}",
                        title = "Mistake Review: ${mistake.lineId}",
                        question = "What is the correct move here?",
                        correctAnswer = mistake.expectedMove,
                        explanation = mistake.explanation,
                        options = listOf(mistake.expectedMove, mistake.userMove)
                            .distinct()
                            .plus(allDrills.take(2).map { it.correctAnswer })
                            .distinct()
                            .shuffled()
                            .take(4),
                        fen = mistake.fen.ifEmpty { null },
                        side = mistake.side,
                        lineId = mistake.lineId,
                        type = dev.counterline.core.model.DrillType.MOVE_RECALL,
                    )
                }

            val regularDrills = allDrills.shuffled().take(QUICK_5_COUNT - mistakeDrills.size)
            val drills = (mistakeDrills + regularDrills).shuffled()

            val now = System.currentTimeMillis()
            _uiState.value = Quick5UiState(
                drills = drills,
                startedMs = now,
            )
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
            reviewNode(
                nodeId = drill.id,
                lineId = drill.lineId ?: drill.side?.name ?: "general",
                side = drill.side ?: Side.WHITE,
                grade = if (correct) ReviewGrade.GOOD else ReviewGrade.FAIL,
                fen = drill.fen ?: "",
                expectedMove = drill.correctAnswer,
                userMove = answer,
                explanation = drill.explanation,
            )
        }
    }

    fun next() {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.drills.size) {
            val elapsed = System.currentTimeMillis() - state.startedMs
            _uiState.update { it.copy(sessionComplete = true, elapsedMs = elapsed) }
            viewModelScope.launch {
                trackSession.end(sessionId, state.totalAnswered, state.correctCount)
            }
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

    fun restart() { loadSession() }
}
