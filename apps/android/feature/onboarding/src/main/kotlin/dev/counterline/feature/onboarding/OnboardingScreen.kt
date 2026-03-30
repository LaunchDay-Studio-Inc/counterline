package dev.counterline.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.counterline.core.designsystem.component.ChessBoard
import dev.counterline.core.designsystem.interaction.FadeSlideUp
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Spacing
import dev.counterline.core.model.SkillLevel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
    ) {
        // Page indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md)
                .semantics { contentDescription = "Page ${state.currentPage + 1} of ${state.totalPages}" },
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(state.totalPages) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.xxs)
                        .size(if (index == state.currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == state.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }

        // Content
        AnimatedContent(
            targetState = state.currentPage,
            modifier = Modifier.weight(1f),
            label = "onboarding_page",
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (page) {
                    0 -> WelcomePage(headline = state.headline, subtitle = state.subtitle)
                    1 -> WhatIsCounterLinePage()
                    2 -> SkillLevelPage(
                        selected = state.selectedSkillLevel,
                        onSelect = viewModel::setSkillLevel,
                    )
                    3 -> StudyFocusPage(
                        selected = state.selectedFocus,
                        onSelect = viewModel::setStudyFocus,
                    )
                    4 -> DailyGoalPage(
                        goal = state.dailyGoal,
                        onGoalChange = viewModel::setDailyGoal,
                    )
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.currentPage > 0) {
                TextButton(onClick = viewModel::previousPage) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (state.currentPage < state.totalPages - 1) {
                Button(onClick = viewModel::nextPage) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                Button(onClick = viewModel::completeOnboarding) {
                    Text("Get Started")
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(headline: String, subtitle: String) {
    Spacer(modifier = Modifier.height(Spacing.xxxl))

    Icon(
        imageVector = Icons.Default.School,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(Spacing.lg))

    Text(
        text = headline,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(Spacing.sm))

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(Spacing.xl))

    Text(
        text = "Welcome to a focused way to learn chess openings.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WhatIsCounterLinePage() {
    Spacer(modifier = Modifier.height(Spacing.lg))

    Text(
        text = "How CounterLine Works",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(Spacing.md))

    val explanations = listOf(
        Triple(Icons.Default.LibraryBooks, "Two Precise Weapons",
            "One opening line for White (Vienna Gambit) and one for Black (Caro-Kann Classical). No bloated databases — just two lines you can actually retain."),
        Triple(Icons.Default.FitnessCenter, "Drill Until Automatic",
            "Spaced-repetition drills adapt to your recall. Moves you know well appear less often. Moves you struggle with come back sooner."),
        Triple(Icons.Default.School, "Know What It Is — and What It Isn't",
            "CounterLine teaches opening lines validated in engine-vs-engine testing. Engine results don't automatically transfer to human play. Your results depend on your study and skill."),
    )

    explanations.forEach { (icon, title, description) ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xxs),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(modifier = Modifier.padding(Spacing.md)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.md))

    // Show the two exit positions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("White: Vienna", style = MaterialTheme.typography.labelMedium)
            ChessBoard(
                fen = "rnb1k1nr/ppp1qppp/8/4Q3/3P1B2/2P2N2/P1P3PP/R3KB1R b KQkq - 0 10",
                modifier = Modifier.size(140.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Black: Caro-Kann", style = MaterialTheme.typography.labelMedium)
            ChessBoard(
                fen = "r2qkbnr/pp1n1pp1/2p1p2p/7P/3P4/3Q1NN1/PPP2PP1/R1B1K2R w KQkq - 0 11",
                modifier = Modifier.size(140.dp),
            )
        }
    }
}

@Composable
private fun SkillLevelPage(
    selected: SkillLevel,
    onSelect: (SkillLevel) -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.lg))

    Text(
        text = "Choose Your Level",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(Spacing.xs))

    Text(
        text = "This controls how much content is visible. You can change it later in Settings.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(Spacing.md))

    val levels = listOf(
        SkillLevel.INTERMEDIATE to "Core moves and basic plans. Recommended for most players.",
        SkillLevel.ADVANCED_CLUB to "Deeper variations and additional drills for club players.",
        SkillLevel.EXPERT_MASTER to "Full deviation coverage, tactical motifs, and transpositions.",
        SkillLevel.ELITE_LAB to "Engine analysis details, proof matrix data, and specialist tuning.",
    )

    levels.forEach { (level, description) ->
        val isSelected = selected == level
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xxs),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
            ),
            onClick = { onSelect(level) },
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = level.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyFocusPage(
    selected: StudyFocus,
    onSelect: (StudyFocus) -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.lg))

    Text(
        text = "What Do You Want to Study First?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(Spacing.xs))

    Text(
        text = "Both lines are always available. This just sets your starting focus.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(Spacing.lg))

    val options = listOf(
        Triple(StudyFocus.WHITE, "White Repertoire",
            "Vienna Gambit Accepted (C29)\nAggressive gambit leading to a nearly winning position."),
        Triple(StudyFocus.BLACK, "Black Repertoire",
            "Caro-Kann Classical 4…Bf5 (B18)\nSolid fortress structure with long-term equality."),
        Triple(StudyFocus.BOTH, "Both Lines",
            "Study White and Black together.\nRecommended for a complete repertoire."),
    )

    options.forEach { (focus, title, description) ->
        FilterChip(
            selected = selected == focus,
            onClick = { onSelect(focus) },
            label = {
                Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingIcon = if (selected == focus) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xxs),
        )
    }
}

@Composable
private fun DailyGoalPage(
    goal: Int,
    onGoalChange: (Int) -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.lg))

    Text(
        text = "Set a Daily Goal",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(Spacing.xs))

    Text(
        text = "How many drill items do you want to complete each day? You can always change this in Settings.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(Spacing.xl))

    Text(
        text = "$goal drills/day",
        style = CounterLineTheme.chessTypography.statHero,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(Spacing.md))

    Slider(
        value = goal.toFloat(),
        onValueChange = { onGoalChange(it.toInt()) },
        valueRange = 5f..50f,
        steps = 8,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Daily drill goal: $goal" },
    )

    Spacer(modifier = Modifier.height(Spacing.xs))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("5 (casual)", style = MaterialTheme.typography.bodySmall)
        Text("50 (intense)", style = MaterialTheme.typography.bodySmall)
    }

    Spacer(modifier = Modifier.height(Spacing.lg))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("You're all set!", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Tap \"Get Started\" to begin learning your repertoire. " +
                    "You can always adjust your settings later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
