package dev.counterline.feature.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.CalculateReadinessUseCase
import dev.counterline.core.domain.DetectWeaknessesUseCase
import dev.counterline.core.domain.GenerateDailyWorkoutUseCase
import dev.counterline.core.domain.GetMasteryUseCase
import dev.counterline.core.domain.GetMistakesUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.model.DailyWorkout
import dev.counterline.core.model.MistakeTheme
import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ReadinessScore
import dev.counterline.core.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val workout: DailyWorkout? = null,
    val whiteReadiness: ReadinessScore? = null,
    val blackReadiness: ReadinessScore? = null,
    val chronicMisses: List<NodeReviewState> = emptyList(),
    val fragileLines: Map<String, Float> = emptyMap(),
    val sideImbalance: Float = 0f,
    val mistakesByTheme: Map<MistakeTheme, Int> = emptyMap(),
    val loading: Boolean = true,
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val generateWorkout: GenerateDailyWorkoutUseCase,
    private val detectWeaknesses: DetectWeaknessesUseCase,
    private val calculateReadiness: CalculateReadinessUseCase,
    private val getSettings: GetSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            val workout = generateWorkout()
            val whiteReadiness = calculateReadiness.forSide(Side.WHITE)
            val blackReadiness = calculateReadiness.forSide(Side.BLACK)
            val chronicMisses = detectWeaknesses.chronicMissNodes().first()
            val fragileLines = detectWeaknesses.fragileLines().first()
            val sideImbalance = detectWeaknesses.sideImbalance().first()
            val mistakesByTheme = detectWeaknesses.mistakesByTheme().first()

            _uiState.value = CoachUiState(
                workout = workout,
                whiteReadiness = whiteReadiness,
                blackReadiness = blackReadiness,
                chronicMisses = chronicMisses,
                fragileLines = fragileLines,
                sideImbalance = sideImbalance,
                mistakesByTheme = mistakesByTheme,
                loading = false,
            )
        }
    }
}
