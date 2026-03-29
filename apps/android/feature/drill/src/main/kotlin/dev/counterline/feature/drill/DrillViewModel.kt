package dev.counterline.feature.drill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetDrillsUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.RecordDrillAttemptUseCase
import dev.counterline.core.domain.ReviewNodeUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.Drill
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

data class DrillUiState(
    val drills: List<Drill> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val showResult: Boolean = false,
    val correctCount: Int = 0,
    val totalAnswered: Int = 0,
    val sessionComplete: Boolean = false,
    val showGrading: Boolean = false,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
    val woodpeckerMode: Boolean = false,
    val woodpeckerRound: Int = 1,
    val woodpeckerMissed: Int = 0,
)

@HiltViewModel
class DrillViewModel @Inject constructor(
    private val getDrills: GetDrillsUseCase,
    private val recordAttempt: RecordDrillAttemptUseCase,
    private val reviewNode: ReviewNodeUseCase,
    private val trackSession: TrackStudySessionUseCase,
    private val getSettings: GetSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrillUiState())
    val uiState: StateFlow<DrillUiState> = _uiState
    private var sessionId: Long = 0
    private val missedDrills = mutableListOf<Drill>()

    init {
        viewModelScope.launch {
            val settings = getSettings().first()
            _uiState.update { it.copy(skillLevel = settings.skillLevel) }
            loadDrills(settings.skillLevel)
            sessionId = trackSession.start(StudyMode.RECALL)
        }
    }

    private suspend fun loadDrills(skillLevel: SkillLevel) {
        val drills = getDrills.forSkillLevel(skillLevel).first().shuffled()
        _uiState.update { it.copy(drills = drills) }
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
                showGrading = true,
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

    fun gradeAndNext(grade: ReviewGrade) {
        val state = _uiState.value
        val drill = state.drills.getOrNull(state.currentIndex)

        if (drill != null) {
            // Track missed items for Woodpecker mode
            if (grade == ReviewGrade.FAIL || grade == ReviewGrade.HARD) {
                missedDrills.add(drill)
            }

            viewModelScope.launch {
                reviewNode(
                    nodeId = drill.id,
                    lineId = drill.lineId ?: drill.side?.name ?: "general",
                    side = drill.side ?: Side.WHITE,
                    grade = grade,
                    fen = drill.fen ?: "",
                    expectedMove = drill.correctAnswer,
                    userMove = state.selectedAnswer ?: "",
                    explanation = drill.explanation,
                )
            }
        }

        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.drills.size) {
            // Woodpecker: if there are missed items, start a new round with them
            if (state.woodpeckerMode && missedDrills.isNotEmpty()) {
                val nextRoundDrills = missedDrills.toList().shuffled()
                missedDrills.clear()
                _uiState.update {
                    it.copy(
                        drills = nextRoundDrills,
                        currentIndex = 0,
                        selectedAnswer = null,
                        showResult = false,
                        showGrading = false,
                        woodpeckerRound = it.woodpeckerRound + 1,
                        woodpeckerMissed = nextRoundDrills.size,
                    )
                }
            } else {
                _uiState.update { it.copy(sessionComplete = true, showGrading = false) }
                viewModelScope.launch {
                    trackSession.end(sessionId, state.totalAnswered, state.correctCount)
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    selectedAnswer = null,
                    showResult = false,
                    showGrading = false,
                )
            }
        }
    }

    fun toggleWoodpecker() {
        _uiState.update { it.copy(woodpeckerMode = !it.woodpeckerMode) }
    }

    fun next() {
        gradeAndNext(
            if (_uiState.value.selectedAnswer == _uiState.value.drills.getOrNull(_uiState.value.currentIndex)?.correctAnswer) {
                ReviewGrade.GOOD
            } else {
                ReviewGrade.FAIL
            },
        )
    }

    fun restart() {
        viewModelScope.launch {
            val settings = getSettings().first()
            val drills = getDrills.forSkillLevel(settings.skillLevel).first().shuffled()
            _uiState.value = DrillUiState(drills = drills, skillLevel = settings.skillLevel)
            sessionId = trackSession.start(StudyMode.RECALL)
        }
    }
}
