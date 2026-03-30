package dev.counterline.feature.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.data.repository.BookmarkRepository
import dev.counterline.core.data.repository.UserNoteRepository
import dev.counterline.core.model.Bookmark
import dev.counterline.core.model.Side
import dev.counterline.core.model.UserNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotebookTab { NOTES, BOOKMARKS }

data class NotebookUiState(
    val tab: NotebookTab = NotebookTab.NOTES,
    val notes: List<UserNote> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val editingNote: UserNote? = null,
    val newNoteText: String = "",
    val newNoteLineId: String = "",
    val newNoteMoveNumber: Int? = null,
    val filterSide: Side? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class NotebookViewModel @Inject constructor(
    private val noteRepo: UserNoteRepository,
    private val bookmarkRepo: BookmarkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotebookUiState())
    val uiState: StateFlow<NotebookUiState> = _uiState

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            val notes = noteRepo.getAll().first()
            val bookmarks = bookmarkRepo.getAll().first()
            _uiState.update {
                it.copy(notes = notes, bookmarks = bookmarks)
            }
        }
    }

    fun selectTab(tab: NotebookTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAll()
            } else {
                val notes = noteRepo.search(query).first()
                _uiState.update { it.copy(notes = notes) }
            }
        }
    }

    fun filterBySide(side: Side?) {
        _uiState.update { it.copy(filterSide = side) }
        viewModelScope.launch {
            val notes = if (side != null) {
                noteRepo.getBySide(side).first()
            } else {
                noteRepo.getAll().first()
            }
            _uiState.update { it.copy(notes = notes) }
        }
    }

    fun updateNewNoteText(text: String) {
        _uiState.update { it.copy(newNoteText = text) }
    }

    fun updateNewNoteLineId(lineId: String) {
        _uiState.update { it.copy(newNoteLineId = lineId) }
    }

    fun saveNewNote() {
        val state = _uiState.value
        if (state.newNoteText.isBlank()) return
        viewModelScope.launch {
            val note = UserNote(
                id = java.util.UUID.randomUUID().toString(),
                lineId = state.newNoteLineId.ifBlank { "general" },
                side = state.filterSide ?: Side.WHITE,
                moveNumber = state.newNoteMoveNumber,
                text = state.newNoteText,
                createdEpochMs = System.currentTimeMillis(),
                updatedEpochMs = System.currentTimeMillis(),
            )
            noteRepo.insert(note)
            _uiState.update { it.copy(newNoteText = "", newNoteLineId = "") }
            loadAll()
        }
    }

    fun deleteNote(note: UserNote) {
        viewModelScope.launch {
            noteRepo.delete(note.id)
            loadAll()
        }
    }

    fun toggleBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepo.delete(bookmark.id)
            loadAll()
        }
    }

    fun addBookmark(lineId: String, nodeId: String, side: Side, label: String) {
        viewModelScope.launch {
            val bm = Bookmark(
                id = java.util.UUID.randomUUID().toString(),
                lineId = lineId,
                nodeId = nodeId,
                side = side,
                label = label,
                createdEpochMs = System.currentTimeMillis(),
            )
            bookmarkRepo.insert(bm)
            loadAll()
        }
    }
}
