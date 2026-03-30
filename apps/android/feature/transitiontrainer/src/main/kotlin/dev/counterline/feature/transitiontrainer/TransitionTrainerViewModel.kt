package dev.counterline.feature.transitiontrainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.GetTransitionPlansUseCase
import dev.counterline.core.domain.TrackStudySessionUseCase
import dev.counterline.core.model.Side
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.TransitionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransitionTrainerUiState(
    val plans: List<TransitionPlan> = emptyList(),
    val selectedPlan: TransitionPlan? = null,
    val selectedSide: Side? = null,
    val showGoals: Boolean = false,
    val showPawnBreaks: Boolean = false,
    val showEndgame: Boolean = false,
)

@HiltViewModel
class TransitionTrainerViewModel @Inject constructor(
    private val getPlans: GetTransitionPlansUseCase,
    private val getSettings: GetSettingsUseCase,
    private val trackSession: TrackStudySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitionTrainerUiState())
    val uiState: StateFlow<TransitionTrainerUiState> = _uiState
    private var sessionId: Long = 0

    init {
        loadPlans()
    }

    private fun loadPlans(side: Side? = null) {
        viewModelScope.launch {
            val plans = if (side != null) {
                getPlans.bySide(side).first()
            } else {
                val whitePlans = getPlans.bySide(Side.WHITE).first()
                val blackPlans = getPlans.bySide(Side.BLACK).first()
                whitePlans + blackPlans
            }
            _uiState.update { it.copy(plans = plans, selectedSide = side) }
        }
    }

    fun filterSide(side: Side?) {
        loadPlans(side)
    }

    fun selectPlan(plan: TransitionPlan) {
        _uiState.update {
            it.copy(
                selectedPlan = plan,
                showGoals = false,
                showPawnBreaks = false,
                showEndgame = false,
            )
        }
        viewModelScope.launch {
            sessionId = trackSession.start(StudyMode.TRANSITION_TRAINER, plan.side)
        }
    }

    fun revealGoals() {
        _uiState.update { it.copy(showGoals = true) }
    }

    fun revealPawnBreaks() {
        _uiState.update { it.copy(showPawnBreaks = true) }
    }

    fun revealEndgame() {
        _uiState.update { it.copy(showEndgame = true) }
    }

    fun backToList() {
        viewModelScope.launch {
            trackSession.end(sessionId, 1, 1)
        }
        _uiState.update {
            it.copy(
                selectedPlan = null,
                showGoals = false,
                showPawnBreaks = false,
                showEndgame = false,
            )
        }
    }
}
