package dev.counterline.feature.repertoire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetRepertoireLinesUseCase
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepertoireUiState(
    val lines: List<RepertoireLine> = emptyList(),
    val selectedSide: Side? = null,
    val selectedLine: RepertoireLine? = null,
)

@HiltViewModel
class RepertoireViewModel @Inject constructor(
    private val getLines: GetRepertoireLinesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepertoireUiState())
    val uiState: StateFlow<RepertoireUiState> = _uiState

    init {
        viewModelScope.launch {
            getLines().collect { lines ->
                _uiState.update { it.copy(lines = lines) }
            }
        }
    }

    fun selectSide(side: Side?) {
        _uiState.update { it.copy(selectedSide = side) }
        viewModelScope.launch {
            val flow = if (side != null) getLines.bySide(side) else getLines()
            flow.collect { lines ->
                _uiState.update { it.copy(lines = lines) }
            }
        }
    }

    fun selectLine(line: RepertoireLine?) {
        _uiState.update { it.copy(selectedLine = line) }
    }
}
