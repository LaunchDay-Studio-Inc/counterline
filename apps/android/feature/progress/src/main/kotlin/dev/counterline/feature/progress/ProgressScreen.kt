package dev.counterline.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.model.ProgressStats

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "Progress Dashboard",
                subtitle = "Your training statistics",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    label = "Streak",
                    value = "${state.stats.currentStreak} days",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Today",
                    value = "${state.stats.drillsCompletedToday} drills",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Due",
                    value = "${state.stats.dueForReview}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Accuracy by Side",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AccuracyBar("White lines", state.stats.whiteLineAccuracy)
                    AccuracyBar("Black lines", state.stats.blackLineAccuracy)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Total drills completed: ${state.stats.totalDrillsCompleted}")
                    Text(text = "Longest streak: ${state.stats.longestStreak} days")
                    Text(text = "Total study time: ${state.stats.totalStudyTimeMinutes} min")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun AccuracyBar(label: String, accuracy: Float) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = "${(accuracy * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = { accuracy.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
