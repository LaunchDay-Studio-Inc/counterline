package dev.counterline.feature.exam

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
fun ExamScreen(
    onBack: () -> Unit = {},
    viewModel: ExamViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Exam Mode") },
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
            // Progress bar
            if (!state.finished && state.questions.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.currentIndex + 1}/${state.questions.size}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            (state.currentIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.finished) {
                // Results
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
                            text = "Exam Complete",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val pct = if (state.questions.isNotEmpty()) {
                            (state.score * 100) / state.questions.size
                        } else 0
                        Text(
                            text = "${state.score} / ${state.questions.size} ($pct%)",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                pct >= 90 -> "Excellent — repertoire mastered!"
                                pct >= 70 -> "Good — keep practicing weak areas"
                                pct >= 50 -> "Fair — review the plans and lines"
                                else -> "Needs work — start with quick start cards"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Done") }
                            Button(onClick = { viewModel.restart() }) { Text("Retake") }
                        }
                    }
                }
            } else {
                val question = state.questions.getOrNull(state.currentIndex)
                if (question != null) {
                    ExamQuestion(
                        drill = question,
                        selectedAnswer = state.selectedAnswer,
                        showResult = state.showResult,
                        onSelect = { viewModel.answer(it) },
                        onNext = { viewModel.next() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamQuestion(
    drill: Drill,
    selectedAnswer: String?,
    showResult: Boolean,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = drill.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (drill.fen != null) {
                ChessBoard(
                    fen = drill.fen,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = drill.question,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))

            drill.options?.forEach { option ->
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

            if (showResult) {
                Spacer(modifier = Modifier.height(12.dp))
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
