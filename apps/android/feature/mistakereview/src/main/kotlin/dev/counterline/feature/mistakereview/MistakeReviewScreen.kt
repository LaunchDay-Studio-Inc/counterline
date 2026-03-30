package dev.counterline.feature.mistakereview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.model.MistakeItem
import dev.counterline.core.model.ReviewGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeReviewScreen(
    onBack: () -> Unit = {},
    viewModel: MistakeReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mistake Review") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Theme grouping toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !state.groupByTheme,
                    onClick = { if (state.groupByTheme) viewModel.toggleGroupByTheme() },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = state.groupByTheme,
                    onClick = { if (!state.groupByTheme) viewModel.toggleGroupByTheme() },
                    label = { Text("By Theme") },
                )
            }
            if (state.selectedTheme != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Theme: ${state.selectedTheme}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(onClick = { viewModel.clearThemeFilter() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Theme group selector
            if (state.groupByTheme && state.selectedTheme == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.themeGroups.forEach { (theme, mistakes) ->
                        Card(
                            onClick = { viewModel.selectTheme(theme) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = theme.replace('_', ' '),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "${mistakes.size} mistakes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else if (state.mistakes.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No mistakes to review",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Great work! Keep drilling to build your repertoire.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onBack) { Text("Back") }
                    }
                }
            } else if (state.sessionComplete) {
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
                            text = "Review Complete",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.resolved} / ${state.total} resolved",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Done") }
                            Button(onClick = { viewModel.restart() }) { Text("Review Again") }
                        }
                    }
                }
            } else {
                // Progress
                Text(
                    text = "${state.currentIndex + 1} / ${state.total}",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val mistake = state.mistakes[state.currentIndex]
                MistakeCard(
                    mistake = mistake,
                    showAnswer = state.showAnswer,
                    onReveal = { viewModel.reveal() },
                    onGrade = { viewModel.grade(it) },
                )
            }
        }
    }
}

@Composable
private fun MistakeCard(
    mistake: MistakeItem,
    showAnswer: Boolean,
    onReveal: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Line: ${mistake.lineId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${mistake.side.name} repertoire",
                    style = MaterialTheme.typography.labelSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    mistake.mistakeTheme?.let { theme ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ) {
                            Text(
                                text = theme.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    mistake.severity?.let { sev ->
                        val sevColor = when (sev.name) {
                            "CRITICAL" -> MaterialTheme.colorScheme.error
                            "MAJOR" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = sevColor.copy(alpha = 0.15f),
                            ),
                        ) {
                            Text(
                                text = sev.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (mistake.fen.isNotEmpty()) {
                ChessBoard(
                    fen = mistake.fen,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "What is the correct move here?",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You played: ${mistake.userMove}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            if (!showAnswer) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reveal Answer")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Correct: ${mistake.expectedMove}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (mistake.explanation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mistake.explanation,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "How well did you know this?",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = { onGrade(ReviewGrade.FAIL) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Again", style = MaterialTheme.typography.labelSmall) }
                    Button(
                        onClick = { onGrade(ReviewGrade.HARD) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) { Text("Hard", style = MaterialTheme.typography.labelSmall) }
                    Button(
                        onClick = { onGrade(ReviewGrade.GOOD) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { Text("Good", style = MaterialTheme.typography.labelSmall) }
                    Button(
                        onClick = { onGrade(ReviewGrade.EASY) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ),
                    ) { Text("Easy", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
