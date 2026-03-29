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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.designsystem.component.StatusBadge
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
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.headline,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Badges
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.badges.forEach { badge ->
                    StatusBadge(text = badge, type = BadgeType.SUCCESS)
                }
            }
        }

        // Daily progress
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.drillsCompletedToday}/${state.dailyGoal}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        LinearProgressIndicator(
                            progress = {
                                (state.drillsCompletedToday.toFloat() / state.dailyGoal.coerceAtLeast(1))
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (state.dueForReview > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.dueForReview} drills due for review",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // Quick actions
        item {
            SectionHeader(title = "Quick Actions")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Browse Repertoire",
                    onClick = onNavigateToRepertoire,
                )
                QuickActionRow(
                    icon = Icons.Default.FitnessCenter,
                    label = "Start Drill Session",
                    onClick = onNavigateToDrill,
                )
                QuickActionRow(
                    icon = Icons.Default.Map,
                    label = "Plans & Patterns",
                    onClick = onNavigateToPlans,
                )
                QuickActionRow(
                    icon = Icons.Default.ShuffleOn,
                    label = "Common Deviations",
                    onClick = onNavigateToDeviations,
                )
                QuickActionRow(
                    icon = Icons.Default.SportsEsports,
                    label = "Model Games",
                    onClick = onNavigateToModelGames,
                )
                QuickActionRow(
                    icon = Icons.Default.Quiz,
                    label = "Exam Mode",
                    onClick = onNavigateToExam,
                )
            }
        }

        // Quick starts
        if (state.quickStarts.isNotEmpty()) {
            item { SectionHeader(title = "Quick Start Cards") }
            items(state.quickStarts) { qs ->
                QuickStartCard(quickStart = qs)
            }
        }

        // Proof summary
        state.proofSummary?.let { proof ->
            item {
                SectionHeader(title = "Evidence Summary")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = proof.headlineResult, style = MaterialTheme.typography.bodyMedium)
                        Text(text = proof.blackSpecialistResult, style = MaterialTheme.typography.bodySmall)
                        Text(text = proof.whiteSpecialistResult, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusBadge(
                            text = "Status: ${proof.statisticalStatus}",
                            type = BadgeType.WARNING,
                        )
                    }
                }
            }
        }

        // Disclaimers
        if (state.disclaimers.isNotEmpty()) {
            item {
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

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuickStartCard(quickStart: QuickStart) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quickStart.seedLine,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChessBoard(
                fen = quickStart.exitFen,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Memory hook: ${quickStart.memoryHook}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            quickStart.threeKeyActions.forEachIndexed { idx, action ->
                Text(
                    text = "${idx + 1}. $action",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
