package dev.counterline.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.core.designsystem.theme.Motion

/**
 * A circular progress ring with animated fill and a center label.
 * Used for mastery visualization, daily goal, and accuracy displays.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    label: String? = null,
    sublabel: String? = null,
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = Motion.tweenMedium(),
        )
    }

    val accessibilityText = label?.let { "$it: ${(progress * 100).toInt()}%" }
        ?: "${(progress * 100).toInt()}% complete"

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = accessibilityText },
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val stroke = strokeWidth.toPx()
            val arcSize = canvasSize - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Progress
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (label != null) {
                Text(
                    text = label,
                    style = CounterLineTheme.chessTypography.statHero,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A smaller inline progress ring for list items and compact stats.
 */
@Composable
fun MiniProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = Motion.tweenMedium(),
        label = "mini_ring",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${(progress * 100).toInt()}%" },
    ) {
        val stroke = strokeWidth.toPx()
        val arcSize = this.size.minDimension - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)

        drawArc(
            color = trackColor, startAngle = -90f, sweepAngle = 360f,
            useCenter = false, topLeft = topLeft, size = Size(arcSize, arcSize),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = color, startAngle = -90f, sweepAngle = 360f * animated,
            useCenter = false, topLeft = topLeft, size = Size(arcSize, arcSize),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
