package dev.counterline.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.SectionHeader
import dev.counterline.core.designsystem.theme.Spacing
import dev.counterline.core.model.DarkMode
import dev.counterline.core.model.SkillLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
    ) {
        item {
            Spacer(modifier = Modifier.height(Spacing.md))
            SectionHeader(title = "Settings")
        }

        // Skill Level
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "Skill Level",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Controls which content is visible. You can change this at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    var skillExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = skillExpanded,
                        onExpandedChange = { skillExpanded = it },
                    ) {
                        TextField(
                            value = state.settings.skillLevel.name.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Level") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(skillExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = skillExpanded, onDismissRequest = { skillExpanded = false }) {
                            SkillLevel.entries.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(level.name.replace('_', ' ').lowercase()
                                            .replaceFirstChar { it.uppercase() })
                                    },
                                    onClick = {
                                        viewModel.setSkillLevel(level)
                                        skillExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Appearance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    // Dark mode
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        TextField(
                            value = state.settings.darkMode.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Theme") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DarkMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                                    },
                                    onClick = {
                                        viewModel.setDarkMode(mode)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Board flip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Flip board (play as Black)",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.settings.boardFlipped,
                            onCheckedChange = { viewModel.setBoardFlipped(it) },
                        )
                    }
                }
            }
        }

        // Training
        item {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "Training",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Text("Daily drill goal: ${state.settings.dailyDrillGoal}")
                    Slider(
                        value = state.settings.dailyDrillGoal.toFloat(),
                        onValueChange = { viewModel.setDailyGoal(it.toInt()) },
                        valueRange = 5f..50f,
                        steps = 8,
                    )
                }
            }
        }

        // Notifications
        item {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Daily reminder", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.settings.notificationsEnabled,
                            onCheckedChange = { viewModel.setNotifications(it) },
                        )
                    }
                }
            }
        }

        // About
        item {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = "CounterLine v1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "An engine-tested opening repertoire trainer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Legal notices
        item {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Legal notices and licenses" },
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        "Legal",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "CounterLine is derived from Stockfish and distributed under the GNU General Public License v3.0 (GPL-3.0).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Open-Source Licenses",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "• Stockfish — GPL-3.0\n• Jetpack Compose — Apache 2.0\n• Dagger/Hilt — Apache 2.0\n• Room — Apache 2.0\n• Kotlin — Apache 2.0\n• Material Design 3 — Apache 2.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Disclaimers",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "Results are specific to the published fixed suite and tested conditions. Engine-vs-engine results do not automatically transfer to human play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}
