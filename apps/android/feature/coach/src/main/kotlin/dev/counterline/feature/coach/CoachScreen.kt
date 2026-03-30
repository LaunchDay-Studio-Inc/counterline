package dev.counterline.feature.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.model.MistakeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    onBack: () -> Unit = {},
    viewModel: CoachViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Personal Coach") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analyzing your progress...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Coach Message
                state.workout?.let { workout ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Today's Plan", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(workout.coachMessage, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recommended: ${workout.recommendedMode.name.replace('_', ' ')} • ${workout.estimatedMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "${workout.items.size} items queued",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // Readiness Scores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.whiteReadiness?.let { readiness ->
                        ReadinessCard(
                            title = "White Readiness",
                            score = readiness.overallScore,
                            overdueItems = readiness.overdueItems,
                            unresolvedMistakes = readiness.unresolvedMistakes,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    state.blackReadiness?.let { readiness ->
                        ReadinessCard(
                            title = "Black Readiness",
                            score = readiness.overallScore,
                            overdueItems = readiness.overdueItems,
                            unresolvedMistakes = readiness.unresolvedMistakes,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Side Imbalance
                if (kotlin.math.abs(state.sideImbalance) > 0.10f) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val stronger = if (state.sideImbalance > 0) "White" else "Black"
                            val weaker = if (state.sideImbalance > 0) "Black" else "White"
                            val gap = (kotlin.math.abs(state.sideImbalance) * 100).toInt()
                            Text("Side Imbalance", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "$stronger is $gap% stronger than $weaker. Consider focusing on $weaker.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // Chronic Misses
                if (state.chronicMisses.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Chronic Weak Spots", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${state.chronicMisses.size} positions failed 3+ times",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            state.chronicMisses.take(5).forEach { node ->
                                Text(
                                    text = "• ${node.nodeId} (${node.lapseCount} lapses, ease: ${"%.1f".format(node.easeFactor)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }

                // Fragile Lines
                if (state.fragileLines.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Fragile Lines", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Lines with mastery >20% below your average:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.fragileLines.entries.take(5).forEach { (lineId, mastery) ->
                                Text(
                                    text = "• $lineId: ${(mastery * 100).toInt()}% mastery",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }

                // Mistakes by Theme
                if (state.mistakesByTheme.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mistake Patterns", style = MaterialTheme.typography.titleSmall)
                            state.mistakesByTheme.entries
                                .sortedByDescending { it.value }
                                .forEach { (theme, count) ->
                                    val label = theme.name.replace('_', ' ').lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                    Text(
                                        text = "$label: $count unresolved",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReadinessCard(
    title: String,
    score: Float,
    overdueItems: Int,
    unresolvedMistakes: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(score * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = { score },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            Text(
                text = "$overdueItems overdue • $unresolvedMistakes mistakes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
