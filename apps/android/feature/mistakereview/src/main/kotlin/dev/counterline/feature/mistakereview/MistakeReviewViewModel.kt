package dev.counterline.feature.mistakereview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetMistakesUseCase
import dev.counterline.core.domain.ResolveMistakeUseCase
import dev.counterline.core.domain.ReviewNodeUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.MistakeItem
import dev.counterline.core.model.ReviewGrade
import dev.counterline.core.model.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MistakeReviewUiState(
    val mistakes: List<MistakeItem> = emptyList(),
    val currentIndex: Int = 0,
    val showAnswer: Boolean = false,
    val sessionComplete: Boolean = false,
    val resolved: Int = 0,
    val total: Int = 0,
    // Phase 2: theme/severity grouping
    val groupByTheme: Boolean = false,
    val themeGroups: Map<String, List<MistakeItem>> = emptyMap(),
    val selectedTheme: String? = null,
)

@HiltViewModel
class MistakeReviewViewModel @Inject constructor(
    private val getMistakes: GetMistakesUseCase,
    private val resolveMistake: ResolveMistakeUseCase,
    private val reviewNode: ReviewNodeUseCase,
    private val trackSession: TrackStudySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MistakeReviewUiState())
    val uiState: StateFlow<MistakeReviewUiState> = _uiState
    private var sessionId: Long = 0

    init {
        viewModelScope.launch {
            sessionId = trackSession.start(StudyMode.MISTAKE_REVIEW)
            val mistakes = getMistakes.unresolved().first()
            val themeGroups = mistakes.groupBy { it.mistakeTheme?.name ?: "UNCATEGORIZED" }
            _uiState.value = MistakeReviewUiState(
                mistakes = mistakes,
                total = mistakes.size,
                themeGroups = themeGroups,
            )
        }
    }

    fun toggleGroupByTheme() {
        _uiState.update { it.copy(groupByTheme = !it.groupByTheme, selectedTheme = null) }
    }

    fun selectTheme(theme: String) {
        val state = _uiState.value
        val themeMistakes = state.themeGroups[theme] ?: return
        _uiState.update {
            it.copy(
                selectedTheme = theme,
                mistakes = themeMistakes,
                total = themeMistakes.size,
                currentIndex = 0,
                showAnswer = false,
                sessionComplete = false,
                resolved = 0,
            )
        }
    }

    fun clearThemeFilter() {
        viewModelScope.launch {
            val mistakes = getMistakes.unresolved().first()
            _uiState.update {
                it.copy(
                    selectedTheme = null,
                    mistakes = mistakes,
                    total = mistakes.size,
                    currentIndex = 0,
                    showAnswer = false,
                    sessionComplete = false,
                    resolved = 0,
                )
            }
        }
    }

    fun reveal() {
        _uiState.update { it.copy(showAnswer = true) }
    }

    fun grade(grade: ReviewGrade) {
        val state = _uiState.value
        val mistake = state.mistakes.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            reviewNode(
                nodeId = mistake.nodeId,
                lineId = mistake.lineId,
                side = mistake.side,
                grade = grade,
                fen = mistake.fen,
                expectedMove = mistake.expectedMove,
                userMove = mistake.userMove,
                explanation = mistake.explanation,
            )
            if (grade >= ReviewGrade.GOOD) {
                resolveMistake(mistake.id)
                _uiState.update { it.copy(resolved = it.resolved + 1) }
            }
        }

        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.mistakes.size) {
            _uiState.update { it.copy(sessionComplete = true) }
            viewModelScope.launch {
                trackSession.end(sessionId, state.total, state.resolved + if (grade >= ReviewGrade.GOOD) 1 else 0)
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    showAnswer = false,
                )
            }
        }
    }

    fun restart() {
        viewModelScope.launch {
            sessionId = trackSession.start(StudyMode.MISTAKE_REVIEW)
            val mistakes = getMistakes.unresolved().first()
            _uiState.value = MistakeReviewUiState(
                mistakes = mistakes,
                total = mistakes.size,
            )
        }
    }
}
