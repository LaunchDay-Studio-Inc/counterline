package dev.counterline.feature.learn

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.StatusBadge
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.RepertoireMove
import dev.counterline.core.model.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    onBack: () -> Unit = {},
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Learn Mode") },
            navigationIcon = {
                IconButton(onClick = {
                    if (state.choosingLine) onBack() else viewModel.backToLines()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (state.choosingLine) {
            LineChooser(
                lines = state.lines,
                selectedSide = state.selectedSide,
                onFilterSide = { viewModel.filterSide(it) },
                onSelectLine = { viewModel.selectLine(it) },
            )
        } else if (state.lineComplete) {
            LineCompleteCard(
                line = state.selectedLine!!,
                onBackToLines = { viewModel.backToLines() },
            )
        } else {
            val line = state.selectedLine!!
            val move = line.moves[state.currentMoveIndex]
            MoveLearnCard(
                line = line,
                move = move,
                moveIndex = state.currentMoveIndex,
                totalMoves = line.moves.size,
                showExplanation = state.showExplanation,
                onShowExplanation = { viewModel.showExplanation() },
                onNext = { viewModel.nextMove() },
            )
        }
    }
}

@Composable
private fun LineChooser(
    lines: List<RepertoireLine>,
    selectedSide: Side?,
    onFilterSide: (Side?) -> Unit,
    onSelectLine: (RepertoireLine) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            FilterChip(
                selected = selectedSide == null,
                onClick = { onFilterSide(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = selectedSide == Side.WHITE,
                onClick = { onFilterSide(Side.WHITE) },
                label = { Text("White") },
            )
            FilterChip(
                selected = selectedSide == Side.BLACK,
                onClick = { onFilterSide(Side.BLACK) },
                label = { Text("Black") },
            )
        }

        Text(
            text = "Choose a line to learn move by move:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lines, key = { it.id }) { line ->
                Card(
                    onClick = { onSelectLine(line) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            StatusBadge(
                                text = if (line.side == Side.WHITE) "White" else "Black",
                                type = if (line.side == Side.WHITE) BadgeType.INFO else BadgeType.SUCCESS,
                            )
                        }
                        Text(
                            text = "${line.moves.size} moves • ${line.eco}",
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
private fun MoveLearnCard(
    line: RepertoireLine,
    move: RepertoireMove,
    moveIndex: Int,
    totalMoves: Int,
    showExplanation: Boolean,
    onShowExplanation: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Line name & progress
        Text(
            text = line.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Move ${moveIndex + 1} of $totalMoves",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Board (show exit FEN as reference — ideal: update per move, uses exit for now)
        ChessBoard(
            fen = line.exitFen,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Move display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val moveLabel = if (move.isWhiteMove) {
                    "${move.moveNumber}. ${move.san}"
                } else {
                    "${move.moveNumber}... ${move.san}"
                }
                Text(
                    text = moveLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = move.purpose,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (showExplanation) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (move.whyThisMove.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Why this move?",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    text = move.whyThisMove,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    if (move.keyPlanCallout.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Key plan",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    text = move.keyPlanCallout,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Next Move")
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onShowExplanation,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Why this move?")
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Got it — Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun LineCompleteCard(
    line: RepertoireLine,
    onBackToLines: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
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
                Text(
                    text = "Line Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${line.moves.size} moves learned",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Memory hook: ${line.memoryHook}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Now drill this line to move it into long-term memory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onBackToLines) {
                    Text("Choose Another Line")
                }
            }
        }
    }
}
