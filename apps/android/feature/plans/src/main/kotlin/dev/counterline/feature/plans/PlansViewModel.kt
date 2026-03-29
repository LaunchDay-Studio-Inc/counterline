package dev.counterline.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetPlansUseCase
import dev.counterline.core.domain.GetThemesUseCase
import dev.counterline.core.model.Plan
import dev.counterline.core.model.Side
import dev.counterline.core.model.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(
    val plans: List<Plan> = emptyList(),
    val themes: List<Theme> = emptyList(),
    val selectedSide: Side? = null,
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val getPlans: GetPlansUseCase,
    private val getThemes: GetThemesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState

    init {
        loadData(null)
    }

    fun selectSide(side: Side?) {
        _uiState.update { it.copy(selectedSide = side) }
        loadData(side)
    }

    private fun loadData(side: Side?) {
        viewModelScope.launch {
            val plansFlow = if (side != null) getPlans.bySide(side) else getPlans()
            plansFlow.collect { plans ->
                _uiState.update { it.copy(plans = plans) }
            }
        }
        viewModelScope.launch {
            getThemes().collect { themes ->
                val filtered = if (side != null) themes.filter { it.side == side } else themes
                _uiState.update { it.copy(themes = filtered) }
            }
        }
    }
}
