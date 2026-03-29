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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.LineCard
import dev.counterline.core.designsystem.component.MoveText
import dev.counterline.core.designsystem.component.SectionHeader
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
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(
            title = "Opening Repertoire",
            subtitle = "Tap a line to explore moves and positions",
        )

        // Side filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LineDetail(line: RepertoireLine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        // Board at exit position
        ChessBoard(
            fen = line.exitFen,
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Exit: move ${line.exitMoveNumber}  |  ${line.evaluationAtExit}",
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "${line.specialistType} (${line.specialistSize})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Memory hook: ${line.memoryHook}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (line.memoryHookBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            line.memoryHookBreakdown.forEach { step ->
                Text(text = "• $step", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Move list
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Moves:", style = MaterialTheme.typography.titleSmall)
        line.moves.forEach { move ->
            MoveText(
                moveNumber = move.moveNumber,
                san = move.san,
                isWhiteMove = move.isWhiteMove,
            )
        }
    }
}
