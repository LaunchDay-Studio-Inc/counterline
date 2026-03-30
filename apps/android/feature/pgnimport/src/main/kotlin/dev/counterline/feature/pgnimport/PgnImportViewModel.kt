package dev.counterline.feature.pgnimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.data.repository.ImportedGameRepository
import dev.counterline.core.domain.ImportPgnUseCase
import dev.counterline.core.model.ImportedGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PgnImportUiState(
    val pgnText: String = "",
    val importedGames: List<ImportedGame> = emptyList(),
    val importing: Boolean = false,
    val importResult: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PgnImportViewModel @Inject constructor(
    private val importPgn: ImportPgnUseCase,
    private val importedGameRepo: ImportedGameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PgnImportUiState())
    val uiState: StateFlow<PgnImportUiState> = _uiState

    init {
        viewModelScope.launch {
            val games = importedGameRepo.getAll().first()
            _uiState.update { it.copy(importedGames = games) }
        }
    }

    fun updatePgnText(text: String) {
        _uiState.update { it.copy(pgnText = text, error = null) }
    }

    fun importPgnText() {
        val pgn = _uiState.value.pgnText
        if (pgn.isBlank()) {
            _uiState.update { it.copy(error = "Please paste PGN text") }
            return
        }

        _uiState.update { it.copy(importing = true, error = null, importResult = null) }

        viewModelScope.launch {
            try {
                val count = importPgn(pgn)
                val games = importedGameRepo.getAll().first()
                _uiState.update {
                    it.copy(
                        importing = false,
                        importResult = "$count game(s) imported",
                        importedGames = games,
                        pgnText = "",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        importing = false,
                        error = "Import failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun deleteGame(game: ImportedGame) {
        viewModelScope.launch {
            importedGameRepo.delete(game.id)
            val games = importedGameRepo.getAll().first()
            _uiState.update { it.copy(importedGames = games) }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(importResult = null) }
    }
}
