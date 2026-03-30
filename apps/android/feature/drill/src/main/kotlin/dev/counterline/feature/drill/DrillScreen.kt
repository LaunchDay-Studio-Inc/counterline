package dev.counterline.feature.drill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.model.Drill
import dev.counterline.core.model.DrillType
import dev.counterline.core.model.ReviewGrade

@Composable
fun DrillScreen(
    viewModel: DrillViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        SectionHeader(
            title = "Drill Session",
            subtitle = if (state.sessionComplete) {
                "Session complete!"
            } else {
                "${state.currentIndex + 1} / ${state.drills.size}"
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Skill level indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Level: ${state.skillLevel.name.replace('_', ' ')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            FilterChip(
                selected = state.woodpeckerMode,
                onClick = { viewModel.toggleWoodpecker() },
                label = { Text("Woodpecker") },
            )
        }

        if (state.woodpeckerMode && state.woodpeckerRound > 1) {
            Text(
                text = "Woodpecker Round ${state.woodpeckerRound} • ${state.woodpeckerMissed} items to review",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Score bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Score: ${state.correctCount}/${state.totalAnswered}",
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.totalAnswered > 0) {
                val pct = (state.correctCount * 100) / state.totalAnswered
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (pct >= 70) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.sessionComplete) {
            SessionCompleteCard(
                correct = state.correctCount,
                total = state.totalAnswered,
                onRestart = { viewModel.restart() },
            )
        } else {
            val drill = state.drills.getOrNull(state.currentIndex)
            if (drill != null) {
                DrillCard(
                    drill = drill,
                    selectedAnswer = state.selectedAnswer,
                    showResult = state.showResult,
                    showGrading = state.showGrading,
                    onSelectAnswer = { viewModel.selectAnswer(it) },
                    onGrade = { viewModel.gradeAndNext(it) },
                    onNext = { viewModel.next() },
                )
            }
        }
    }
}

@Composable
private fun DrillCard(
    drill: Drill,
    selectedAnswer: String?,
    showResult: Boolean,
    showGrading: Boolean,
    onSelectAnswer: (String) -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = drill.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = drill.type.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            val drillFen = drill.fen
            if (drillFen != null) {
                ChessBoard(
                    fen = drillFen,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = drill.question,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(12.dp))

            val drillOptions = drill.options
            if (drillOptions != null) {
                drillOptions.forEach { option ->
                    val isSelected = option == selectedAnswer
                    val isCorrect = option == drill.correctAnswer

                    val containerColor = when {
                        showResult && isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                        showResult && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        onClick = { onSelectAnswer(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            } else {
                if (!showResult) {
                    Button(
                        onClick = { onSelectAnswer(drill.correctAnswer) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reveal Answer")
                    }
                }
            }

            if (showResult) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Answer: ${drill.correctAnswer}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = drill.explanation,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (showGrading) {
                    Text(
                        text = "How confident were you?",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        GradeButton("Again", ReviewGrade.FAIL, MaterialTheme.colorScheme.error, Modifier.weight(1f), onGrade)
                        GradeButton("Hard", ReviewGrade.HARD, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f), onGrade)
                        GradeButton("Good", ReviewGrade.GOOD, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onGrade)
                        GradeButton("Easy", ReviewGrade.EASY, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onGrade)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    grade: ReviewGrade,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onGrade: (ReviewGrade) -> Unit,
) {
    Button(
        onClick = { onGrade(grade) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SessionCompleteCard(
    correct: Int,
    total: Int,
    onRestart: () -> Unit,
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
                text = "Session Complete!",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$correct / $total correct",
                style = MaterialTheme.typography.titleLarge,
            )
            if (total > 0) {
                val pct = (correct * 100) / total
                Text(
                    text = "$pct% accuracy",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRestart) {
                Text("Start New Session")
            }
        }
    }
}
