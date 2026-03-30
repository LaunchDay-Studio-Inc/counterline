package dev.counterline.feature.preppack

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.model.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepPackScreen(
    onBack: () -> Unit = {},
    viewModel: PrepPackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Preparation Pack") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Side filter
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.selectedSide == null,
                        onClick = { viewModel.filterSide(null) },
                        label = { Text("Both") },
                    )
                    FilterChip(
                        selected = state.selectedSide == Side.WHITE,
                        onClick = { viewModel.filterSide(Side.WHITE) },
                        label = { Text("White") },
                    )
                    FilterChip(
                        selected = state.selectedSide == Side.BLACK,
                        onClick = { viewModel.filterSide(Side.BLACK) },
                        label = { Text("Black") },
                    )
                }
            }

            // Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.generatePrepPack() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Generate Pack")
                    }
                    OutlinedButton(
                        onClick = { viewModel.takeSnapshot() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Take Snapshot")
                    }
                }
            }

            // Prep pack result
            state.prepPack?.let { pack ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = pack.label,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${pack.lineIds.size} lines included",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = pack.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Cheat sheet
            if (state.cheatSheet.isNotBlank()) {
                item {
                    Text(
                        text = "Cheat Sheet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(
                            text = state.cheatSheet,
                            modifier = Modifier
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }
            }

            // Snapshots
            if (state.snapshots.isNotEmpty()) {
                item {
                    Text(
                        text = "Repertoire Snapshots (${state.snapshots.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.snapshots, key = { it.id }) { snapshot ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = snapshot.label,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${snapshot.lineCount} lines • ${snapshot.totalMoves} moves",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Snapshot diff
            state.snapshotDiff?.let { diff ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Text(
                            text = diff,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
