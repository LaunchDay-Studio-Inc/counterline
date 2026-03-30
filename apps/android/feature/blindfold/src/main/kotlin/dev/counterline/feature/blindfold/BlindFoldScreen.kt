package dev.counterline.feature.blindfold

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.StatusBadge
import dev.counterline.core.model.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlindFoldScreen(
    onBack: () -> Unit = {},
    viewModel: BlindFoldViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Blindfold Recall") },
            navigationIcon = {
                IconButton(onClick = {
                    if (state.currentLine != null) viewModel.backToLines() else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (!state.available) {
            LockedContent(requiredLevel = state.requiredLevel.name)
        } else if (state.currentLine == null) {
            LineChooser(state = state, onSelect = { viewModel.selectLine(it) })
        } else if (state.sessionComplete) {
            ResultsContent(state = state, onBack = { viewModel.backToLines() })
        } else {
            BlindFoldDrillContent(
                state = state,
                onSubmit = { viewModel.submitMove(it) },
                onNext = { viewModel.nextMove() },
            )
        }
    }
}

@Composable
private fun LockedContent(requiredLevel: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Blindfold Recall",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "This advanced mode is available at the $requiredLevel skill level and above.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Practice recalling move orders without seeing the board.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LineChooser(
    state: BlindFoldUiState,
    onSelect: (dev.counterline.core.model.RepertoireLine) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Choose a line to recall from memory — no board shown.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.lines, key = { it.id }) { line ->
                Card(
                    onClick = { onSelect(line) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = line.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusBadge(
                                text = if (line.side == Side.WHITE) "White" else "Black",
                                type = if (line.side == Side.WHITE) BadgeType.INFO else BadgeType.SUCCESS,
                            )
                        }
                        Text(
                            text = "${line.moves.size} moves",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BlindFoldDrillContent(
    state: BlindFoldUiState,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
) {
    val line = state.currentLine ?: return
    val move = line.moves.getOrNull(state.currentMoveIndex) ?: return
    var inputText by remember(state.currentMoveIndex) { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        // Progress
        LinearProgressIndicator(
            progress = { (state.currentMoveIndex + 1).toFloat() / line.moves.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Move ${state.currentMoveIndex + 1} of ${line.moves.size}", style = MaterialTheme.typography.labelMedium)
            Text("Score: ${state.score}/${state.totalAttempts}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // No board — just the line name and move number
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = line.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val prompt = if (move.isWhiteMove) "Move ${move.moveNumber}. ?" else "Move ${move.moveNumber}... ?"
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "What is the next move?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (state.feedback == null) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Move (SAN)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSubmit(inputText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank(),
            ) {
                Text("Submit")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isCorrect == true) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.feedback ?: "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Next Move")
            }
        }
    }
}

@Composable
private fun ResultsContent(
    state: BlindFoldUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Blindfold Recall Complete", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${state.currentLine?.name}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${state.score} / ${state.totalAttempts}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                val pct = if (state.totalAttempts > 0) (state.score * 100 / state.totalAttempts) else 0
                Text("$pct% accuracy", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Choose Another Line") }
    }
}
