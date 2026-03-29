package dev.counterline.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.engine.EngineStrengthProfile
import dev.counterline.core.model.RepertoireLine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    onBack: () -> Unit = {},
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Play Against CounterLine") },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.endPracticeSession()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (state.showLineSelector) {
            LineSelector(
                lines = state.availableLines,
                onSelectLine = { viewModel.selectLine(it) },
                mode = state.mode,
                onSetMode = { viewModel.setPracticeMode(it) },
                strength = state.strengthProfile,
                onSetStrength = { viewModel.setStrength(it) },
            )
        } else if (state.sessionOver) {
            SessionResult(
                movesPlayed = state.movesPlayed,
                correctMoves = state.correctMoves,
                onBack = onBack,
            )
        } else {
            ActivePractice(
                state = state,
                onDismissFeedback = { viewModel.dismissFeedback() },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineSelector(
    lines: List<RepertoireLine>,
    onSelectLine: (RepertoireLine) -> Unit,
    mode: PracticeMode,
    onSetMode: (PracticeMode) -> Unit,
    strength: EngineStrengthProfile,
    onSetStrength: (EngineStrengthProfile) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionHeader(title = "Choose a Line")
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Practice mode toggle
        item {
            Text("Practice Mode", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == PracticeMode.LINE_LOCK,
                    onClick = { onSetMode(PracticeMode.LINE_LOCK) },
                    label = { Text("Line Lock") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                )
                FilterChip(
                    selected = mode == PracticeMode.DEVIATION,
                    onClick = { onSetMode(PracticeMode.DEVIATION) },
                    label = { Text("Deviation Mode") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Strength selector
        item {
            Text("Engine Strength", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EngineStrengthProfile.entries
                    .filter { it != EngineStrengthProfile.ANALYSIS && it != EngineStrengthProfile.DEEP_ANALYSIS }
                    .forEach { profile ->
                        FilterChip(
                            selected = strength == profile,
                            onClick = { onSetStrength(profile) },
                            label = { Text(profile.displayName) },
                        )
                    }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Line cards
        items(lines) { line ->
            Card(
                onClick = { onSelectLine(line) },
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = line.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${line.side.name} • ${line.eco} • ${line.family}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (line.memoryHook.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = line.memoryHook,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePractice(
    state: PracticeUiState,
    onDismissFeedback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Status bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.repertoireLine?.name ?: "Practice",
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.isInsideRepertoire) {
                Text(
                    text = "In repertoire",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Deviation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Mode badge
        Text(
            text = "Mode: ${state.mode.name.replace('_', ' ')} • ${state.strengthProfile.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Board
        ChessBoard(
            fen = state.currentFen,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Moves: ${state.movesPlayed}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Correct: ${state.correctMoves}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Move history
        if (state.moveHistorySan.isNotEmpty()) {
            Text(
                text = state.moveHistorySan.chunked(2).mapIndexed { i, pair ->
                    "${i + 1}. ${pair.joinToString(" ")}"
                }.joinToString("  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Engine thinking indicator
        if (state.isEngineThinking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Engine is thinking...")
            }
        }

        // Feedback card
        state.lastFeedback?.let { feedback ->
            Spacer(modifier = Modifier.height(8.dp))
            FeedbackCard(feedback = feedback, onDismiss = onDismissFeedback)
        }

        // Deviation explanation
        state.deviationExplanation?.let { explanation ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Deviation",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onDismissFeedback) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: dev.counterline.core.engine.MoveFeedback,
    onDismiss: () -> Unit,
) {
    val containerColor = if (feedback.isCorrect) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (feedback.isCorrect) "Correct!" else "Incorrect",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feedback.explanation,
                style = MaterialTheme.typography.bodySmall,
            )
            feedback.engineNote?.let { note ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            feedback.whyNot?.let { whyNot ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = whyNot,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss) {
                Text(if (feedback.isCorrect) "Continue" else "Try Again")
            }
        }
    }
}

@Composable
private fun SessionResult(
    movesPlayed: Int,
    correctMoves: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Session Complete",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$correctMoves / $movesPlayed correct",
            style = MaterialTheme.typography.titleLarge,
        )
        if (movesPlayed > 0) {
            val pct = (correctMoves * 100) / movesPlayed
            Text(
                text = "$pct% accuracy",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}
