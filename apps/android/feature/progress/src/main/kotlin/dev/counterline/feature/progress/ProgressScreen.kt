package dev.counterline.feature.progress

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.MasteryBar
import dev.counterline.core.designsystem.component.ProgressRing
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.designsystem.component.StatHeroCard
import dev.counterline.core.designsystem.component.StatusBadge
import dev.counterline.core.designsystem.component.StreakIndicator
import dev.counterline.core.designsystem.interaction.FadeSlideUp
import dev.counterline.core.designsystem.theme.ChessShapes
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Elevation
import dev.counterline.core.designsystem.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Spacer(modifier = Modifier.height(Spacing.md))
            SectionHeader(
                title = "Progress Dashboard",
                subtitle = "Your training statistics",
            )
        }

        // ── Hero stat cards ──
        item {
            FadeSlideUp {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    StatHeroCard(
                        value = "${state.currentStreak}",
                        label = "day streak",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = CounterLineTheme.chessColors.streakActive,
                        modifier = Modifier.weight(1f),
                    )
                    StatHeroCard(
                        value = "${state.stats.drillsCompletedToday}",
                        label = "today",
                        modifier = Modifier.weight(1f),
                    )
                    StatHeroCard(
                        value = "${state.stats.dueForReview}",
                        label = "due",
                        icon = Icons.Default.Schedule,
                        accentColor = if (state.stats.dueForReview > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Total study time ──
        item {
            FadeSlideUp(delayMs = 50) {
                val hours = state.totalStudyTimeMs / 3_600_000
                val minutes = (state.totalStudyTimeMs / 60_000) % 60
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Total study time", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${hours}h ${minutes}m",
                            style = CounterLineTheme.chessTypography.statCompact,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // ── Mastery rings (White + Black) ──
        item {
            FadeSlideUp(delayMs = 100) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                    ) {
                        Text(
                            text = "Repertoire Mastery",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ProgressRing(
                                progress = state.whiteMastery,
                                size = 100.dp,
                                strokeWidth = 9.dp,
                                label = "${(state.whiteMastery * 100).toInt()}%",
                                sublabel = "White",
                                progressColor = CounterLineTheme.chessColors.whiteWeapon,
                            )
                            ProgressRing(
                                progress = state.blackMastery,
                                size = 100.dp,
                                strokeWidth = 9.dp,
                                label = "${(state.blackMastery * 100).toInt()}%",
                                sublabel = "Black",
                                progressColor = CounterLineTheme.chessColors.blackWeapon,
                            )
                        }
                    }
                }
            }
        }

        // ── Accuracy bars ──
        item {
            FadeSlideUp(delayMs = 150) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Accuracy by Side",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        MasteryBar("White lines", state.stats.whiteLineAccuracy)
                        MasteryBar("Black lines", state.stats.blackLineAccuracy)
                    }
                }
            }
        }

        // ── Badges ──
        if (state.badges.isNotEmpty()) {
            item {
                FadeSlideUp(delayMs = 200) {
                    Column {
                        SectionHeader(title = "Earned Badges")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                        ) {
                            state.badges.forEach { badge ->
                                StatusBadge(text = badge.title, type = BadgeType.SUCCESS)
                            }
                        }
                    }
                }
            }
        }

        // ── Exam certificates ──
        item {
            FadeSlideUp(delayMs = 250) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Exam Certificates",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        state.whiteExamBest?.let { exam ->
                            Text(text = "White: ${(exam.accuracy * 100).toInt()}% accuracy, ${if (exam.passed) "PASSED" else "not passed"}")
                        } ?: Text(text = "White: No exam taken yet", style = MaterialTheme.typography.bodySmall)
                        state.blackExamBest?.let { exam ->
                            Text(text = "Black: ${(exam.accuracy * 100).toInt()}% accuracy, ${if (exam.passed) "PASSED" else "not passed"}")
                        } ?: Text(text = "Black: No exam taken yet", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ── Weakest nodes ──
        if (state.weakestNodes.isNotEmpty()) {
            item {
                SectionHeader(title = "Weakest Areas")
            }
            items(state.weakestNodes) { node ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = node.nodeId,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${node.lapseCount} lapses",
                            style = MaterialTheme.typography.labelSmall,
                            color = CounterLineTheme.chessColors.incorrectMove,
                        )
                    }
                }
            }
        }

        // ── Unresolved mistakes ──
        if (state.unresolvedMistakes > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Column(modifier = Modifier.padding(start = Spacing.sm)) {
                            Text(
                                text = "${state.unresolvedMistakes} unresolved mistakes",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Review them in Mistake Review mode",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        // ── Summary ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(text = "Total drills completed: ${state.stats.totalDrillsCompleted}")
                    Text(text = "Longest streak: ${state.stats.longestStreak} days")
                    Text(text = "Total study time: ${state.stats.totalStudyTimeMinutes} min")
                }
            }
        }

        // ── Phase 5: Readiness Scores ──
        if (state.whiteReadiness != null || state.blackReadiness != null) {
            item {
                FadeSlideUp(delayMs = 300) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Tournament Readiness",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.semantics { heading() },
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                state.whiteReadiness?.let { r ->
                                    ProgressRing(
                                        progress = r.overallScore,
                                        size = 80.dp,
                                        strokeWidth = 7.dp,
                                        label = "${(r.overallScore * 100).toInt()}%",
                                        sublabel = "White",
                                        progressColor = CounterLineTheme.chessColors.whiteWeapon,
                                    )
                                }
                                state.blackReadiness?.let { r ->
                                    ProgressRing(
                                        progress = r.overallScore,
                                        size = 80.dp,
                                        strokeWidth = 7.dp,
                                        label = "${(r.overallScore * 100).toInt()}%",
                                        sublabel = "Black",
                                        progressColor = CounterLineTheme.chessColors.blackWeapon,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            state.whiteReadiness?.let { r ->
                                Text(
                                    text = r.coachMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Phase 5: Side Imbalance ──
        if (state.sideImbalance > 0.15f) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Side Imbalance Warning",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val imbalancePct = (state.sideImbalance * 100).toInt()
                        Text(
                            text = "Your White and Black training differ by $imbalancePct%. Balance your sessions.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        // ── Phase 5: Chronic Miss Nodes ──
        if (state.chronicMissNodes.isNotEmpty()) {
            item {
                SectionHeader(title = "Chronic Problem Areas")
            }
            items(state.chronicMissNodes) { nodeId ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        text = nodeId,
                        modifier = Modifier.padding(Spacing.sm),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // ── Phase 5: Weekly Training Arcs ──
        if (state.weeklyArcs.isNotEmpty()) {
            item {
                SectionHeader(title = "Weekly Training Arcs")
            }
            items(state.weeklyArcs) { arc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        Text(
                            text = "Week of ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(arc.weekStartEpochMs))}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${arc.drillCount} drills",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${(arc.avgAccuracy * 100).toInt()}% accuracy",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${arc.studyMinutes}m",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        MasteryBar("Progress", arc.avgAccuracy)
                    }
                }
            }
        }

        // ── Phase 5: Achievements ──
        if (state.achievements.isNotEmpty()) {
            item {
                SectionHeader(title = "Achievements")
            }
            items(state.achievements) { achievement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = if (achievement.unlockedEpochMs != null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${achievement.icon} ${achievement.title}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (achievement.unlockedEpochMs != null) {
                            StatusBadge(text = "Unlocked", type = BadgeType.SUCCESS)
                        } else {
                            StatusBadge(text = "Locked", type = BadgeType.WARNING)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(Spacing.md)) }
    }
}

