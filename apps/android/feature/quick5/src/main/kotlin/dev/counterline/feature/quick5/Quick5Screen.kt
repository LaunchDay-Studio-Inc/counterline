package dev.counterline.feature.quick5

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import dev.counterline.core.model.Drill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Quick5Screen(
    onBack: () -> Unit = {},
    viewModel: Quick5ViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Quick 5") },
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
            if (state.sessionComplete) {
                val seconds = state.elapsedMs / 1000
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
                        Text("Quick 5 Done!", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.correctCount} / ${state.totalAnswered} correct",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Time: ${seconds}s",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Done") }
                            Button(onClick = { viewModel.restart() }) { Text("Again") }
                        }
                    }
                }
            } else if (state.drills.isNotEmpty()) {
                // Progress
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.currentIndex + 1}/${state.drills.size}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            (state.currentIndex + 1).toFloat() / state.drills.size
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                val drill = state.drills[state.currentIndex]
                QuickDrillCard(
                    drill = drill,
                    selectedAnswer = state.selectedAnswer,
                    showResult = state.showResult,
                    onSelect = { viewModel.selectAnswer(it) },
                    onNext = { viewModel.next() },
                )
            }
        }
    }
}

@Composable
private fun QuickDrillCard(
    drill: Drill,
    selectedAnswer: String?,
    showResult: Boolean,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = drill.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val drillFen = drill.fen
            if (drillFen != null) {
                ChessBoard(
                    fen = drillFen,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(text = drill.question, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            val drillOptions = drill.options
            if (drillOptions != null) {
                drillOptions.forEach { option ->
                    val isCorrect = option == drill.correctAnswer
                    val isSelected = option == selectedAnswer
                    val color = when {
                        showResult && isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                        showResult && isSelected -> MaterialTheme.colorScheme.errorContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Card(
                        onClick = { if (!showResult) onSelect(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = color),
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                if (!showResult) {
                    Button(
                        onClick = { onSelect(drill.correctAnswer) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Reveal Answer") }
                }
            }

            if (showResult) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = drill.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text("Next")
                }
            }
        }
    }
}
