package dev.counterline.feature.modelgames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetModelGamesUseCase
import dev.counterline.core.model.ModelGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelGamesUiState(
    val games: List<ModelGame> = emptyList(),
    val expandedGameId: String? = null,
)

@HiltViewModel
class ModelGamesViewModel @Inject constructor(
    private val getModelGames: GetModelGamesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelGamesUiState())
    val uiState: StateFlow<ModelGamesUiState> = _uiState

    init {
        viewModelScope.launch {
            getModelGames().collect { games ->
                _uiState.update { it.copy(games = games) }
            }
        }
    }

    fun toggleGame(id: String) {
        _uiState.update {
            it.copy(expandedGameId = if (it.expandedGameId == id) null else id)
        }
    }
}
