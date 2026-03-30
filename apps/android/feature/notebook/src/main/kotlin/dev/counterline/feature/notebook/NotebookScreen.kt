package dev.counterline.feature.notebook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.model.Bookmark as BookmarkModel
import dev.counterline.core.model.Side
import dev.counterline.core.model.UserNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    onBack: () -> Unit = {},
    viewModel: NotebookViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notebook") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        TabRow(selectedTabIndex = state.tab.ordinal) {
            Tab(
                selected = state.tab == NotebookTab.NOTES,
                onClick = { viewModel.selectTab(NotebookTab.NOTES) },
                text = { Text("Notes") },
                icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
            )
            Tab(
                selected = state.tab == NotebookTab.BOOKMARKS,
                onClick = { viewModel.selectTab(NotebookTab.BOOKMARKS) },
                text = { Text("Bookmarks") },
                icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            )
        }

        when (state.tab) {
            NotebookTab.NOTES -> NotesContent(
                notes = state.notes,
                newNoteText = state.newNoteText,
                newNoteLineId = state.newNoteLineId,
                searchQuery = state.searchQuery,
                filterSide = state.filterSide,
                onSearchChange = { viewModel.updateSearch(it) },
                onFilterSide = { viewModel.filterBySide(it) },
                onNoteTextChange = { viewModel.updateNewNoteText(it) },
                onNoteLineIdChange = { viewModel.updateNewNoteLineId(it) },
                onSaveNote = { viewModel.saveNewNote() },
                onDeleteNote = { viewModel.deleteNote(it) },
            )
            NotebookTab.BOOKMARKS -> BookmarksContent(
                bookmarks = state.bookmarks,
                onRemoveBookmark = { viewModel.toggleBookmark(it) },
            )
        }
    }
}

@Composable
private fun NotesContent(
    notes: List<UserNote>,
    newNoteText: String,
    newNoteLineId: String,
    searchQuery: String,
    filterSide: Side?,
    onSearchChange: (String) -> Unit,
    onFilterSide: (Side?) -> Unit,
    onNoteTextChange: (String) -> Unit,
    onNoteLineIdChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onDeleteNote: (UserNote) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search notes") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Side filter
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterSide == null,
                onClick = { onFilterSide(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = filterSide == Side.WHITE,
                onClick = { onFilterSide(Side.WHITE) },
                label = { Text("White") },
            )
            FilterChip(
                selected = filterSide == Side.BLACK,
                onClick = { onFilterSide(Side.BLACK) },
                label = { Text("Black") },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Add note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Add Note", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newNoteLineId,
                    onValueChange = onNoteLineIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Line ID (optional)") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = onNoteTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note text") },
                    maxLines = 4,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSaveNote,
                    enabled = newNoteText.isNotBlank(),
                ) {
                    Text("Save Note")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Notes list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { note ->
                NoteCard(note = note, onDelete = { onDeleteNote(note) })
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: UserNote,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Line: ${note.lineId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    note.moveNumber?.let { mn ->
                        Text(
                            text = "Move $mn",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete note",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
            )
            val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(note.updatedEpochMs))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarksContent(
    bookmarks: List<BookmarkModel>,
    onRemoveBookmark: (BookmarkModel) -> Unit,
) {
    if (bookmarks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No bookmarks yet",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bookmark positions during study to quickly return to them.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(bookmarks, key = { it.id }) { bookmark ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookmark.label,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${bookmark.side.name} • ${bookmark.lineId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onRemoveBookmark(bookmark) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove bookmark",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
