package dev.counterline.feature.repertoire

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.LineCard
import dev.counterline.core.designsystem.component.MoveText
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.designsystem.interaction.FadeSlideUp
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Spacing
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.Side

@Composable
fun RepertoireScreen(
    viewModel: RepertoireViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
    ) {
        Spacer(modifier = Modifier.height(Spacing.md))
        FadeSlideUp {
            SectionHeader(
                title = "Opening Repertoire",
                subtitle = "Tap a line to explore moves and positions",
            )
        }

        // Side filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(vertical = Spacing.xs),
        ) {
            FilterChip(
                selected = state.selectedSide == null,
                onClick = { viewModel.selectSide(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = state.selectedSide == Side.WHITE,
                onClick = { viewModel.selectSide(Side.WHITE) },
                label = { Text("White") },
            )
            FilterChip(
                selected = state.selectedSide == Side.BLACK,
                onClick = { viewModel.selectSide(Side.BLACK) },
                label = { Text("Black") },
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(state.lines, key = { it.id }) { line ->
                LineCard(
                    name = line.name,
                    eco = line.eco,
                    seedLine = line.seedLine,
                    scorePct = line.screeningScorePct,
                    onClick = {
                        viewModel.selectLine(if (state.selectedLine?.id == line.id) null else line)
                    },
                )
                AnimatedVisibility(visible = state.selectedLine?.id == line.id) {
                    LineDetail(line = line)
                }
            }
            item { Spacer(modifier = Modifier.height(Spacing.md)) }
        }
    }
}

@Composable
private fun LineDetail(line: RepertoireLine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Board at exit position
            ChessBoard(
                fen = line.exitFen,
                modifier = Modifier.fillMaxWidth(0.7f),
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "Exit: move ${line.exitMoveNumber}  |  ${line.evaluationAtExit}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "${line.specialistType} (${line.specialistSize})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Memory hook: ${line.memoryHook}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (line.memoryHookBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                line.memoryHookBreakdown.forEach { step ->
                    Text(text = "• $step", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Move list
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Moves:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
            line.moves.forEach { move ->
                MoveText(
                    moveNumber = move.moveNumber,
                    san = move.san,
                    isWhiteMove = move.isWhiteMove,
                )
            }
        }
    }
}
