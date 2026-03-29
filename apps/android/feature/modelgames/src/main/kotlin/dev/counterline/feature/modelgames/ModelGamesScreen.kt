package dev.counterline.feature.modelgames

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
            title = { Text("Model Games") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.games, key = { it.id }) { game ->
                GameCard(
                    game = game,
                    expanded = state.expandedGameId == game.id,
                    onClick = { viewModel.toggleGame(game.id) },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun GameCard(
    game: ModelGame,
    expanded: Boolean,
    onClick: () -> Unit,
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
            }
        }
    }
}
