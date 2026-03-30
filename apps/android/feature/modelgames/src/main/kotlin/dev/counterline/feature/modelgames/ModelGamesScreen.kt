package dev.counterline.feature.modelgames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.StatusBadge
import dev.counterline.core.model.ModelGame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelGamesScreen(
    onBack: () -> Unit = {},
    viewModel: ModelGamesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (state.gtmActive) "Guess the Move" else "Model Games") },
            navigationIcon = {
                IconButton(onClick = {
                    if (state.gtmActive) viewModel.exitGtm() else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (state.gtmActive) {
            GuessTheMoveContent(
                state = state,
                onUpdateGuess = { viewModel.updateGuess(it) },
                onSubmitGuess = { viewModel.submitGuess() },
                onNextAnnotation = { viewModel.nextAnnotation() },
                onExit = { viewModel.exitGtm() },
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        expanded = state.expandedGameId == game.id,
                        onClick = { viewModel.toggleGame(game.id) },
                        onStartGtm = { viewModel.startGuessTheMove(game) },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun GuessTheMoveContent(
    state: ModelGamesUiState,
    onUpdateGuess: (String) -> Unit,
    onSubmitGuess: () -> Unit,
    onNextAnnotation: () -> Unit,
    onExit: () -> Unit,
) {
    val game = state.gtmGame ?: return

    Column(modifier = Modifier.padding(16.dp)) {
        if (state.gtmComplete) {
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
                    Text("Game Complete!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${state.gtmScore} / ${state.gtmMaxScore} points",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    val pct = if (state.gtmMaxScore > 0) (state.gtmScore * 100) / state.gtmMaxScore else 0
                    Text(
                        text = "$pct% accuracy",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            pct >= 80 -> "Excellent understanding of the plans!"
                            pct >= 60 -> "Good grasp — review the missed positions"
                            pct >= 40 -> "Decent — study the key themes more closely"
                            else -> "Keep studying — focus on the key plans"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onExit) { Text("Back to Games") }
                }
            }
        } else {
            val ann = game.annotations.getOrNull(state.gtmAnnotationIndex) ?: return

            // Progress
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.gtmAnnotationIndex + 1}/${game.annotations.size}",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = {
                        (state.gtmAnnotationIndex + 1).toFloat() / game.annotations.size.coerceAtLeast(1)
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Score: ${state.gtmScore}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Move ${ann.moveNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "What would you play here?",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    if (!state.gtmGuessRevealed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.gtmUserGuess,
                            onValueChange = onUpdateGuess,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Your move (SAN)") },
                            placeholder = { Text("e.g. Nf3, e4, Bb5...") },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onSubmitGuess,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.gtmUserGuess.isNotBlank(),
                        ) {
                            Text("Submit Guess")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        val lastScore = state.gtmScoreHistory.lastOrNull() ?: 0
                        val scoreLabel = when (lastScore) {
                            3 -> "Exact match! +3"
                            2 -> "Close! +2"
                            1 -> "Reasonable try +1"
                            else -> "Not quite +0"
                        }
                        val scoreColor = when (lastScore) {
                            3 -> MaterialTheme.colorScheme.primary
                            2 -> MaterialTheme.colorScheme.tertiary
                            1 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                        StatusBadge(
                            text = scoreLabel,
                            type = when (lastScore) {
                                3 -> BadgeType.SUCCESS
                                2 -> BadgeType.INFO
                                else -> BadgeType.WARNING
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = ann.comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (ann.evaluation != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Eval: ${ann.evaluation}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNextAnnotation,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Next Position")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: ModelGame,
    expanded: Boolean,
    onClick: () -> Unit,
    onStartGtm: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(
                    text = game.result,
                    type = when {
                        game.result.contains("1-0") || game.result.contains("0-1") -> BadgeType.SUCCESS
                        else -> BadgeType.INFO
                    },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${game.opening} • ${game.moveCount} moves",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Theme: ${game.keyTheme}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Eval progression: ${game.evaluationProgression}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                game.annotations.forEach { annotation ->
                    Text(
                        text = "Move ${annotation.moveNumber}: ${annotation.comment}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                    if (annotation.evaluation != null) {
                        Text(
                            text = "  eval: ${annotation.evaluation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (game.annotations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onStartGtm) {
                        Text("Guess the Move")
                    }
                }
            }
        }
    }
}
