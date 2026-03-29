package dev.counterline.feature.plans

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.model.Plan
import dev.counterline.core.model.Side
import dev.counterline.core.model.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onBack: () -> Unit = {},
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Plans & Patterns") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        // Side filter
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
                label = { Text("White") },
            )
            FilterChip(
                selected = state.selectedSide == Side.BLACK,
                onClick = { viewModel.selectSide(Side.BLACK) },
                label = { Text("Black") },
            )
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Plans section
            item { SectionHeader(title = "Plans") }
            items(state.plans, key = { it.id }) { plan ->
                PlanCard(plan = plan)
            }

            // Themes section
            if (state.themes.isNotEmpty()) {
                item { SectionHeader(title = "Strategic Themes") }
                items(state.themes, key = { it.id }) { theme ->
                    ThemeCard(theme = theme)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = plan.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Priority ${plan.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeCard(theme: Theme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = theme.title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodySmall,
            )
            if (theme.occurrenceRate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Frequency: ${theme.occurrenceRate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
