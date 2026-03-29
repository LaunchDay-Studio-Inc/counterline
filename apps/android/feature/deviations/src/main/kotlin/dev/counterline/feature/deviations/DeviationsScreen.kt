package dev.counterline.feature.deviations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviationsScreen(
    onBack: () -> Unit = {},
    viewModel: DeviationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Common Deviations") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            FilterChip(
                selected = state.selectedSide == null,
                onClick = { viewModel.selectSide(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = state.selectedSide == Side.WHITE,
                onClick = { viewModel.selectSide(Side.WHITE) },
                label = { Text("vs White lines") },
            )
            FilterChip(
                selected = state.selectedSide == Side.BLACK,
                onClick = { viewModel.selectSide(Side.BLACK) },
                label = { Text("vs Black lines") },
            )
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.deviations, key = { it.id }) { dev ->
                DeviationCard(deviation = dev)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DeviationCard(deviation: Deviation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = deviation.deviationName,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = deviation.move,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = deviation.description,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Response:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = deviation.response,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
