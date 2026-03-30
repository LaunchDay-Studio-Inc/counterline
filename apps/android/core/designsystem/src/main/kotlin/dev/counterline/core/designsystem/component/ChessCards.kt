package dev.counterline.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.counterline.core.designsystem.theme.ChessShapes
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Elevation
import dev.counterline.core.designsystem.theme.Motion
import dev.counterline.core.designsystem.theme.Spacing

/**
 * A premium weapon summary card (White or Black repertoire).
 * Shows the weapon name, mastery ring, and key stats at a glance.
 */
@Composable
fun WeaponCard(
    title: String,
    subtitle: String,
    mastery: Float,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    memoryHook: String? = null,
    dueCount: Int = 0,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ChessShapes.weaponCard,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mastery ring
            MiniProgressRing(
                progress = mastery,
                size = 56.dp,
                strokeWidth = 5.dp,
                color = accentColor,
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (memoryHook != null) {
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = memoryHook,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        maxLines = 1,
                    )
                }
            }

            if (dueCount > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$dueCount",
                        style = CounterLineTheme.chessTypography.statCompact,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * A hero stat card with a large number, label, and optional trend indicator.
 * Used on the Progress Dashboard.
 */
@Composable
fun StatHeroCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        modifier = modifier,
        shape = ChessShapes.statCard,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
            }
            Text(
                text = value,
                style = CounterLineTheme.chessTypography.statCompact,
                color = accentColor,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A streak day indicator row — 7 dots showing the last 7 days of activity.
 */
@Composable
fun StreakIndicator(
    activeDays: List<Boolean>,
    modifier: Modifier = Modifier,
) {
    val chessColors = CounterLineTheme.chessColors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        activeDays.takeLast(7).forEachIndexed { index, active ->
            val color by animateColorAsState(
                targetValue = if (active) chessColors.streakActive else chessColors.streakInactive,
                animationSpec = Motion.tweenShort(),
                label = "streak_$index",
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(ChessShapes.badge)
                    .background(color)
                    .semantics {
                        contentDescription = if (active) "Day ${index + 1}: active" else "Day ${index + 1}: inactive"
                    },
            )
        }
    }
}

/**
 * Animated mastery bar with color that changes based on mastery level.
 */
@Composable
fun MasteryBar(
    label: String,
    mastery: Float,
    modifier: Modifier = Modifier,
) {
    val chessColors = CounterLineTheme.chessColors
    val barColor by animateColorAsState(
        targetValue = when {
            mastery >= 0.8f -> chessColors.masteryHigh
            mastery >= 0.5f -> chessColors.masteryMedium
            else -> chessColors.masteryLow
        },
        animationSpec = Motion.tweenShort(),
        label = "mastery_color",
    )

    val animatedWidth by animateFloatAsState(
        targetValue = mastery.coerceIn(0f, 1f),
        animationSpec = Motion.tweenMedium(),
        label = "mastery_width",
    )

    Column(modifier = modifier.padding(vertical = Spacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${(mastery * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(ChessShapes.badge)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .height(8.dp)
                    .clip(ChessShapes.badge)
                    .background(barColor),
            )
        }
    }
}

/**
 * An animated evaluation bar showing advantage for White or Black.
 */
@Composable
fun EvalBar(
    evalCp: Int,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val chessColors = CounterLineTheme.chessColors
    val whiteRatio = ((evalCp + 500).coerceIn(0, 1000).toFloat() / 1000f)
    val animatedRatio by animateFloatAsState(
        targetValue = whiteRatio,
        animationSpec = Motion.tweenMedium(),
        label = "eval_bar",
    )

    Box(
        modifier = modifier
            .width(24.dp)
            .height(height)
            .clip(ChessShapes.badge)
            .background(chessColors.evalBlack)
            .semantics {
                contentDescription = when {
                    evalCp > 50 -> "White advantage: +${evalCp / 100f}"
                    evalCp < -50 -> "Black advantage: ${evalCp / 100f}"
                    else -> "Equal position"
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height * animatedRatio)
                .align(Alignment.BottomCenter)
                .background(chessColors.evalWhite),
        )
    }
}
