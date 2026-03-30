package dev.counterline.feature.modelgames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetModelGamesUseCase
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.ModelGameAnnotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelGamesUiState(
    val games: List<ModelGame> = emptyList(),
    val expandedGameId: String? = null,
    // Guess-the-move mode
    val gtmActive: Boolean = false,
    val gtmGame: ModelGame? = null,
    val gtmAnnotationIndex: Int = 0,
    val gtmUserGuess: String = "",
    val gtmGuessRevealed: Boolean = false,
    val gtmScore: Int = 0,
    val gtmMaxScore: Int = 0,
    val gtmComplete: Boolean = false,
    val gtmScoreHistory: List<Int> = emptyList(), // score per annotation
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

    fun startGuessTheMove(game: ModelGame) {
        _uiState.update {
            it.copy(
                gtmActive = true,
                gtmGame = game,
                gtmAnnotationIndex = 0,
                gtmUserGuess = "",
                gtmGuessRevealed = false,
                gtmScore = 0,
                gtmMaxScore = game.annotations.size * 3,
                gtmComplete = false,
                gtmScoreHistory = emptyList(),
            )
        }
    }

    fun updateGuess(guess: String) {
        _uiState.update { it.copy(gtmUserGuess = guess) }
    }

    fun submitGuess() {
        val state = _uiState.value
        val game = state.gtmGame ?: return
        val ann = game.annotations.getOrNull(state.gtmAnnotationIndex) ?: return
        val guess = state.gtmUserGuess.trim()

        // Scoring: 3 = exact match, 2 = correct piece/direction, 1 = reasonable, 0 = wrong
        val moveText = "Move ${ann.moveNumber}"
        val points = when {
            guess.equals(ann.comment.substringBefore(" ").trim(), ignoreCase = true) -> 3
            guess.isNotBlank() && ann.comment.contains(guess, ignoreCase = true) -> 2
            guess.isNotBlank() -> 1
            else -> 0
        }

        _uiState.update {
            it.copy(
                gtmGuessRevealed = true,
                gtmScore = it.gtmScore + points,
                gtmScoreHistory = it.gtmScoreHistory + points,
            )
        }
    }

    fun nextAnnotation() {
        val state = _uiState.value
        val game = state.gtmGame ?: return
        val nextIdx = state.gtmAnnotationIndex + 1
        if (nextIdx >= game.annotations.size) {
            _uiState.update { it.copy(gtmComplete = true) }
        } else {
            _uiState.update {
                it.copy(
                    gtmAnnotationIndex = nextIdx,
                    gtmUserGuess = "",
                    gtmGuessRevealed = false,
                )
            }
        }
    }

    fun exitGtm() {
        _uiState.update {
            it.copy(
                gtmActive = false,
                gtmGame = null,
                gtmComplete = false,
            )
        }
    }
}
