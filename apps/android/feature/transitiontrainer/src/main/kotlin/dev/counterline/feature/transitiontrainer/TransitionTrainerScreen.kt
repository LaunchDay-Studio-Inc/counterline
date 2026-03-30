package dev.counterline.feature.transitiontrainer

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.counterline.core.designsystem.component.BadgeType
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.component.StatusBadge
import dev.counterline.core.model.Side
import dev.counterline.core.model.TransitionPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionTrainerScreen(
    onBack: () -> Unit = {},
    viewModel: TransitionTrainerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Transition Trainer") },
            navigationIcon = {
                IconButton(onClick = {
                    if (state.selectedPlan != null) viewModel.backToList() else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (state.selectedPlan == null) {
            PlanListContent(
                plans = state.plans,
                selectedSide = state.selectedSide,
                onFilterSide = { viewModel.filterSide(it) },
                onSelectPlan = { viewModel.selectPlan(it) },
            )
        } else {
            PlanDetailContent(
                plan = state.selectedPlan!!,
                showGoals = state.showGoals,
                showPawnBreaks = state.showPawnBreaks,
                showEndgame = state.showEndgame,
                onRevealGoals = { viewModel.revealGoals() },
                onRevealPawnBreaks = { viewModel.revealPawnBreaks() },
                onRevealEndgame = { viewModel.revealEndgame() },
                onBack = { viewModel.backToList() },
            )
        }
    }
}

@Composable
private fun PlanListContent(
    plans: List<TransitionPlan>,
    selectedSide: Side?,
    onFilterSide: (Side?) -> Unit,
    onSelectPlan: (TransitionPlan) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            FilterChip(selected = selectedSide == null, onClick = { onFilterSide(null) }, label = { Text("All") })
            FilterChip(selected = selectedSide == Side.WHITE, onClick = { onFilterSide(Side.WHITE) }, label = { Text("White") })
            FilterChip(selected = selectedSide == Side.BLACK, onClick = { onFilterSide(Side.BLACK) }, label = { Text("Black") })
        }

        Text(
            text = "Learn how your opening leads into the middlegame.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plans, key = { it.id }) { plan ->
                Card(
                    onClick = { onSelectPlan(plan) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Line: ${plan.lineId}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            StatusBadge(
                                text = plan.side.name,
                                type = if (plan.side == Side.WHITE) BadgeType.INFO else BadgeType.SUCCESS,
                            )
                        }
                        Text(
                            text = "${plan.strategicGoals.size} goals • ${plan.pawnBreaks.size} pawn breaks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlanDetailContent(
    plan: TransitionPlan,
    showGoals: Boolean,
    showPawnBreaks: Boolean,
    showEndgame: Boolean,
    onRevealGoals: () -> Unit,
    onRevealPawnBreaks: () -> Unit,
    onRevealEndgame: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Tabiya board
        Text("Tabiya Position", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ChessBoard(
            fen = plan.tabiyaFen,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Strategic Goals
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Strategic Goals", style = MaterialTheme.typography.titleSmall)
                if (showGoals) {
                    plan.strategicGoals.forEachIndexed { idx, goal ->
                        Text(
                            text = "${idx + 1}. $goal",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                    OutlinedButton(onClick = onRevealGoals, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Reveal Goals")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Pawn Breaks
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pawn Breaks", style = MaterialTheme.typography.titleSmall)
                if (showPawnBreaks) {
                    plan.pawnBreaks.forEach { pawnBreak ->
                        Text(
                            text = "• $pawnBreak",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                    OutlinedButton(onClick = onRevealPawnBreaks, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Reveal Pawn Breaks")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Endgame Tendency
        if (plan.endgameTendency.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Endgame Tendency", style = MaterialTheme.typography.titleSmall)
                    if (showEndgame) {
                        Text(
                            text = plan.endgameTendency,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        OutlinedButton(onClick = onRevealEndgame, modifier = Modifier.padding(top = 4.dp)) {
                            Text("Reveal")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Plans")
        }
    }
}
