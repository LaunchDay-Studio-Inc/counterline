package dev.counterline.feature.tacticalmotifs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalMotifScreen(
    onBack: () -> Unit = {},
    viewModel: TacticalMotifViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Tactical Motifs") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when (state.mode) {
            MotifMode.CHOOSING -> ChoosingContent(
                onStartStandard = { viewModel.startSession() },
                onStartWoodpecker = { viewModel.startWoodpeckerSession() },
            )

            MotifMode.SOLVING -> SolvingContent(
                state = state,
                onSubmit = { viewModel.submitAnswer(it) },
                onNext = { viewModel.nextMotif() },
            )

            MotifMode.RESULTS -> ResultsContent(
                state = state,
                onReset = { viewModel.reset() },
            )
        }
    }
}

@Composable
private fun ChoosingContent(
    onStartStandard: () -> Unit,
    onStartWoodpecker: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Tactical Motif Training",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Practice tactics from your actual repertoire positions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartStandard,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("Standard Session (10 puzzles)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onStartWoodpecker,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("Woodpecker Cycle (repeat misses)")
        }
    }
}

@Composable
private fun SolvingContent(
    state: TacticalMotifUiState,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
) {
    val motif = state.motifs.getOrNull(state.currentIndex) ?: return
    var inputText by remember(state.currentIndex) { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        // Progress
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1).toFloat() / state.motifs.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Puzzle ${state.currentIndex + 1} of ${state.motifs.size}",
                style = MaterialTheme.typography.labelMedium,
            )
            if (state.maxCycles > 1) {
                Text(
                    text = "Cycle ${state.cycleNumber} of ${state.maxCycles}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = "Score: ${state.score}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Board
        ChessBoard(
            fen = motif.fen,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Motif type hint
        Text(
            text = motif.motifType,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!state.showSolution) {
            // Input
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Your move (SAN)") },
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
            // Solution feedback
            val correct = motif.solutionSan.firstOrNull()?.equals(state.userAnswer, ignoreCase = true) == true
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (correct) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (correct) "Correct!" else "Incorrect",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Solution: ${motif.solutionSan.joinToString(" ")}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = motif.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ResultsContent(
    state: TacticalMotifUiState,
    onReset: () -> Unit,
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Session Complete", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${state.score} / ${state.total}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                val pct = if (state.total > 0) (state.score * 100 / state.total) else 0
                Text(
                    text = "$pct% accuracy",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.maxCycles > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed ${state.cycleNumber} Woodpecker cycles",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onReset) {
            Text("Train Again")
        }
    }
}
