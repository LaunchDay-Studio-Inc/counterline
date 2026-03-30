package dev.counterline.feature.drill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.MiniProgressRing
import dev.counterline.core.designsystem.component.ProgressRing
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.designsystem.interaction.AnimatedEntrance
import dev.counterline.core.designsystem.interaction.CelebrationPop
import dev.counterline.core.designsystem.interaction.ShakeOnError
import dev.counterline.core.designsystem.interaction.rememberHapticFeedback
import dev.counterline.core.designsystem.theme.ChessShapes
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Elevation
import dev.counterline.core.designsystem.theme.Spacing
import dev.counterline.core.model.Drill
import dev.counterline.core.model.DrillType
import dev.counterline.core.model.ReviewGrade

@Composable
fun DrillScreen(
    viewModel: DrillViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHapticFeedback()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
    ) {
        SectionHeader(
            title = "Drill Session",
            subtitle = if (state.sessionComplete) {
                "Session complete!"
            } else {
                "${state.currentIndex + 1} / ${state.drills.size}"
            },
        )

        Spacer(modifier = Modifier.height(Spacing.xxs))

        // Skill level + Woodpecker toggle
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

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Score with mini progress ring
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Score: ${state.correctCount} of ${state.totalAnswered} correct"
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.totalAnswered > 0) {
                    MiniProgressRing(
                        progress = state.correctCount.toFloat() / state.totalAnswered,
                        size = 32.dp,
                        strokeWidth = 3.dp,
                        color = if (state.correctCount * 100 / state.totalAnswered >= 70)
                            CounterLineTheme.chessColors.correctMove
                        else CounterLineTheme.chessColors.incorrectMove,
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                }
                Text(
                    text = "Score: ${state.correctCount}/${state.totalAnswered}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (state.totalAnswered > 0) {
                val pct = (state.correctCount * 100) / state.totalAnswered
                Text(
                    text = "$pct%",
                    style = CounterLineTheme.chessTypography.statCompact,
                    color = if (pct >= 70) CounterLineTheme.chessColors.correctMove
                    else CounterLineTheme.chessColors.incorrectMove,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        if (state.sessionComplete) {
            CelebrationPop(trigger = true) {
                SessionCompleteCard(
                    correct = state.correctCount,
                    total = state.totalAnswered,
                    onRestart = { viewModel.restart() },
                )
            }
        } else {
            val drill = state.drills.getOrNull(state.currentIndex)
            if (drill != null) {
                AnimatedEntrance(visible = true) {
                    DrillCard(
                        drill = drill,
                        selectedAnswer = state.selectedAnswer,
                        showResult = state.showResult,
                        showGrading = state.showGrading,
                        onSelectAnswer = { answer ->
                            viewModel.selectAnswer(answer)
                            if (answer == drill.correctAnswer) {
                                haptics.performConfirm()
                            } else {
                                haptics.performReject()
                            }
                        },
                        onGrade = { grade ->
                            haptics.performClick()
                            viewModel.gradeAndNext(grade)
                        },
                        onNext = { viewModel.next() },
                    )
                }
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
    val isIncorrect = showResult && selectedAnswer != null && selectedAnswer != drill.correctAnswer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.high),
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = drill.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = drill.type.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            val drillFen = drill.fen
            if (drillFen != null) {
                ChessBoard(
                    fen = drillFen,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Text(
                text = drill.question,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            val drillOptions = drill.options
            if (drillOptions != null) {
                ShakeOnError(trigger = isIncorrect) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        drillOptions.forEach { option ->
                            val isSelected = option == selectedAnswer
                            val isCorrect = option == drill.correctAnswer

                            val containerColor = when {
                                showResult && isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                                showResult && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerLow
                            }

                            Card(
                                onClick = { if (!showResult) onSelectAnswer(option) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = ChessShapes.drillOption,
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(Spacing.sm),
                                )
                            }
                        }
                    }
                }
            } else {
                if (!showResult) {
                    Button(
                        onClick = { onSelectAnswer(drill.correctAnswer) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("Reveal Answer")
                    }
                }
            }

            if (showResult) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        Text(
                            text = "Answer: ${drill.correctAnswer}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = drill.explanation,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))

                if (showGrading) {
                    Text(
                        text = "How confident were you?",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        GradeButton("Again", ReviewGrade.FAIL, CounterLineTheme.chessColors.incorrectMove, Modifier.weight(1f), onGrade)
                        GradeButton("Hard", ReviewGrade.HARD, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f), onGrade)
                        GradeButton("Good", ReviewGrade.GOOD, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onGrade)
                        GradeButton("Easy", ReviewGrade.EASY, CounterLineTheme.chessColors.correctMove, Modifier.weight(1f), onGrade)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
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
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
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
        shape = ChessShapes.weaponCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Session Complete!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            if (total > 0) {
                val pct = (correct * 100) / total
                ProgressRing(
                    progress = correct.toFloat() / total,
                    size = 100.dp,
                    strokeWidth = 10.dp,
                    label = "$pct%",
                    sublabel = "accuracy",
                    progressColor = if (pct >= 70)
                        CounterLineTheme.chessColors.correctMove
                    else CounterLineTheme.chessColors.incorrectMove,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
            Text(
                text = "$correct / $total correct",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text("Start New Session")
            }
        }
    }
}
