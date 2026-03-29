package dev.counterline.feature.deviations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetDeviationsUseCase
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviationsUiState(
    val deviations: List<Deviation> = emptyList(),
    val selectedSide: Side? = null,
)

@HiltViewModel
class DeviationsViewModel @Inject constructor(
    private val getDeviations: GetDeviationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviationsUiState())
    val uiState: StateFlow<DeviationsUiState> = _uiState

    init { loadData(null) }

    fun selectSide(side: Side?) {
        _uiState.update { it.copy(selectedSide = side) }
        loadData(side)
    }

    private fun loadData(side: Side?) {
        viewModelScope.launch {
            val flow = if (side != null) getDeviations.bySide(side) else getDeviations()
            flow.collect { devs ->
                _uiState.update { it.copy(deviations = devs) }
            }
        }
    }
}
