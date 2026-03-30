package dev.counterline.core.designsystem.interaction

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import dev.counterline.core.designsystem.theme.Motion
import kotlinx.coroutines.delay

/**
 * Trigger a haptic feedback pattern on the current view.
 * Safe to call from any composable context.
 */
@Composable
fun rememberHapticFeedback(): HapticController {
    val view = LocalView.current
    return remember(view) {
        HapticController(
            performConfirm = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            },
            performReject = {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            },
            performClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            },
            performLongPress = {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            },
        )
    }
}

data class HapticController(
    val performConfirm: () -> Unit,
    val performReject: () -> Unit,
    val performClick: () -> Unit,
    val performLongPress: () -> Unit,
)

/**
 * Animate a composable appearing with a subtle scale + fade entrance.
 * Used for drill cards, result reveals, and list items.
 */
@Composable
fun AnimatedEntrance(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.tweenMedium()) + expandVertically(Motion.tweenMedium()),
        exit = fadeOut(Motion.tweenShort()) + shrinkVertically(Motion.tweenShort()),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * A pop-in scale animation for milestone celebrations.
 * Scales from 0 → 1.1 → 1.0 with a spring.
 */
@Composable
fun CelebrationPop(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            scale.snapTo(0f)
            scale.animateTo(1.12f, tween(Motion.DURATION_MEDIUM))
            scale.animateTo(1f, Motion.springBouncy())
        }
    }

    Box(modifier = modifier.scale(scale.value)) {
        content()
    }
}

/**
 * Subtle shake animation for incorrect answers.
 * Horizontal displacement: ±6dp over 3 oscillations.
 */
@Composable
fun ShakeOnError(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            repeat(3) {
                offset.animateTo(8f, tween(50))
                offset.animateTo(-8f, tween(50))
            }
            offset.animateTo(0f, tween(50))
        }
    }

    Box(
        modifier = modifier.graphicsLayer { translationX = offset.value },
    ) {
        content()
    }
}

/**
 * A pulse animation for drawing attention to a CTA.
 * Scales between 1.0 and 1.05 continuously until stopped.
 */
@Composable
fun PulseAttention(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var pulsing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pulsing && active) 1.04f else 1f,
        animationSpec = tween(600),
        label = "pulse",
        finishedListener = { pulsing = !pulsing },
    )

    LaunchedEffect(active) {
        if (active) {
            delay(300)
            pulsing = true
        } else {
            pulsing = false
        }
    }

    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

/**
 * Fade-slide-up entrance for screen content.
 * Use as a wrapper around LazyColumn items or screen roots.
 */
@Composable
fun FadeSlideUp(
    visible: Boolean = true,
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())
            show = true
        } else {
            show = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = Motion.tweenMedium(),
        label = "fade",
    )
    val translationY by animateFloatAsState(
        targetValue = if (show) 0f else 24f,
        animationSpec = Motion.tweenMedium(),
        label = "slide",
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        },
    ) {
        content()
    }
}
