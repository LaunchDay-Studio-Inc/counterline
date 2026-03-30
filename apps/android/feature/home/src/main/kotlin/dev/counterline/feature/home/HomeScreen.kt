package dev.counterline.feature.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.MiniProgressRing
import dev.counterline.core.designsystem.component.ProgressRing
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.designsystem.component.StatusBadge
import dev.counterline.core.designsystem.component.WeaponCard
import dev.counterline.core.designsystem.interaction.FadeSlideUp
import dev.counterline.core.designsystem.interaction.PulseAttention
import dev.counterline.core.designsystem.theme.ChessShapes
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Elevation
import dev.counterline.core.designsystem.theme.Spacing
import dev.counterline.core.model.QuickStart
import dev.counterline.core.model.Side

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToRepertoire: () -> Unit = {},
    onNavigateToDrill: () -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    onNavigateToDeviations: () -> Unit = {},
    onNavigateToModelGames: () -> Unit = {},
    onNavigateToExam: () -> Unit = {},
    onNavigateToLearn: () -> Unit = {},
    onNavigateToMistakeReview: () -> Unit = {},
    onNavigateToQuick5: () -> Unit = {},
    onNavigateToPractice: () -> Unit = {},
    onNavigateToTacticalMotifs: () -> Unit = {},
    onNavigateToTransitionTrainer: () -> Unit = {},
    onNavigateToBlindFold: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onNavigateToNotebook: () -> Unit = {},
    onNavigateToPgnImport: () -> Unit = {},
    onNavigateToPrepPack: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // ── Hero header ──
        item {
            FadeSlideUp {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Text(
                        text = state.headline,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Daily progress ring + smart CTA ──
        item {
            FadeSlideUp(delayMs = 50) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChessShapes.weaponCard,
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val dailyProgress = state.drillsCompletedToday.toFloat() /
                            state.dailyGoal.coerceAtLeast(1)
                        ProgressRing(
                            progress = dailyProgress.coerceIn(0f, 1f),
                            size = 80.dp,
                            strokeWidth = 8.dp,
                            label = "${state.drillsCompletedToday}",
                            sublabel = "of ${state.dailyGoal}",
                            progressColor = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (state.dueForReview > 0) {
                                Spacer(modifier = Modifier.height(Spacing.xxs))
                                PulseAttention(active = state.dueForReview > 5) {
                                    Text(
                                        text = "${state.dueForReview} drills due for review",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            FilledTonalButton(onClick = onNavigateToDrill) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("Start Drilling")
                            }
                        }
                    }
                }
            }
        }

        // ── Weapon summary cards ──
        item {
            FadeSlideUp(delayMs = 100) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionHeader(title = "Your Weapons")
                    WeaponCard(
                        title = "White Repertoire",
                        subtitle = "Vienna Gambit Accepted (C29)",
                        mastery = 0f, // Will be populated from state
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = CounterLineTheme.chessColors.whiteWeapon,
                        onClick = onNavigateToRepertoire,
                    )
                    WeaponCard(
                        title = "Black Repertoire",
                        subtitle = "Caro-Kann Classical (B18)",
                        mastery = 0f,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = CounterLineTheme.chessColors.blackWeapon,
                        onClick = onNavigateToRepertoire,
                    )
                }
            }
        }

        // ── Focused quick actions (grouped by purpose, not a wall of buttons) ──
        item {
            FadeSlideUp(delayMs = 150) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SectionHeader(title = "Train")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        QuickActionChip(
                            icon = Icons.Default.RocketLaunch,
                            label = "Quick 5",
                            onClick = onNavigateToQuick5,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.Quiz,
                            label = "Exam",
                            onClick = onNavigateToExam,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.SportsEsports,
                            label = "Play",
                            onClick = onNavigateToPractice,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    SectionHeader(title = "Study")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        QuickActionChip(
                            icon = Icons.Default.Map,
                            label = "Plans",
                            onClick = onNavigateToPlans,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.ShuffleOn,
                            label = "Deviations",
                            onClick = onNavigateToDeviations,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.Lightbulb,
                            label = "Learn",
                            onClick = onNavigateToLearn,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        QuickActionChip(
                            icon = Icons.Default.SportsEsports,
                            label = "Games",
                            onClick = onNavigateToModelGames,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.Replay,
                            label = "Mistakes",
                            onClick = onNavigateToMistakeReview,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.Extension,
                            label = "Tactics",
                            onClick = onNavigateToTacticalMotifs,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        QuickActionChip(
                            icon = Icons.Default.SwapHoriz,
                            label = "Transitions",
                            onClick = onNavigateToTransitionTrainer,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.VisibilityOff,
                            label = "Blindfold",
                            onClick = onNavigateToBlindFold,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.Psychology,
                            label = "Coach",
                            onClick = onNavigateToCoach,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Lifetime weapon tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        QuickActionChip(
                            icon = Icons.Default.EditNote,
                            label = "Notebook",
                            onClick = onNavigateToNotebook,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.UploadFile,
                            label = "Import PGN",
                            onClick = onNavigateToPgnImport,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionChip(
                            icon = Icons.Default.Backpack,
                            label = "Prep Pack",
                            onClick = onNavigateToPrepPack,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ── Badges ──
        if (state.badges.isNotEmpty()) {
            item {
                FadeSlideUp(delayMs = 200) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        state.badges.forEach { badge ->
                            StatusBadge(text = badge, type = BadgeType.SUCCESS)
                        }
                    }
                }
            }
        }

        // ── Quick start cards ──
        if (state.quickStarts.isNotEmpty()) {
            item {
                FadeSlideUp(delayMs = 250) {
                    SectionHeader(title = "Quick Start Cards")
                }
            }
            items(state.quickStarts) { qs ->
                FadeSlideUp(delayMs = 300) {
                    QuickStartCard(quickStart = qs)
                }
            }
        }

        // ── Evidence summary ──
        state.proofSummary?.let { proof ->
            item {
                FadeSlideUp(delayMs = 350) {
                    Column {
                        SectionHeader(title = "Evidence Summary")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.md)) {
                                Text(
                                    text = proof.headlineResult,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.height(Spacing.xxs))
                                Text(
                                    text = proof.blackSpecialistResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = proof.whiteSpecialistResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                StatusBadge(
                                    text = "Status: ${proof.statisticalStatus}",
                                    type = BadgeType.WARNING,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Disclaimers ──
        if (state.disclaimers.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Disclaimers")
                    state.disclaimers.forEach { disclaimer ->
                        Text(
                            text = "• $disclaimer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(Spacing.xl)) }
    }
}

/**
 * Compact action chip — replaces the old full-width OutlinedButton wall.
 * Shows icon + short label in a tappable card with 48dp min touch target.
 */
@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QuickStartCard(quickStart: QuickStart) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = quickStart.lineName,
                    style = MaterialTheme.typography.titleMedium,
                )
                StatusBadge(
                    text = if (quickStart.side == Side.WHITE) "White" else "Black",
                    type = if (quickStart.side == Side.WHITE) BadgeType.INFO else BadgeType.SUCCESS,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = quickStart.seedLine,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            ChessBoard(
                fen = quickStart.exitFen,
                modifier = Modifier.fillMaxWidth(0.65f),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Memory hook: ${quickStart.memoryHook}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            quickStart.threeKeyActions.forEachIndexed { idx, action ->
                Text(
                    text = "${idx + 1}. $action",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
