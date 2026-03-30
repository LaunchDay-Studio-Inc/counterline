package dev.counterline.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Motion tokens for consistent, premium-feeling animations.
 *
 * Design principles:
 * - Fast enough to feel responsive (never > 500ms for UI transitions)
 * - Smooth spring physics for interactive elements
 * - Ease-out for entrances, ease-in for exits
 * - Respect reduced-motion preferences (checked at call site)
 */
@Immutable
object Motion {
    // ── Duration tokens ──
    /** Micro: state change feedback (color, icon swap) */
    const val DURATION_MICRO = 100
    /** Short: chips, toggles, small reveals */
    const val DURATION_SHORT = 200
    /** Medium: card transitions, progress fills */
    const val DURATION_MEDIUM = 350
    /** Long: screen transitions, hero animations */
    const val DURATION_LONG = 500
    /** Extended: onboarding sequences, celebration */
    const val DURATION_EXTENDED = 800

    // ── Easing tokens ──
    /** Standard M3 emphasised easing */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard = FastOutSlowInEasing

    // ── Spring specs ──
    /** Bouncy spring for celebrations and milestone pops */
    fun <T> springBouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Gentle spring for smooth transitions */
    fun <T> springGentle() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Snappy spring for interactive feedback */
    fun <T> springSnappy() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    // ── Tween specs ──
    fun <T> tweenMedium() = tween<T>(DURATION_MEDIUM, easing = EmphasizedDecelerate)
    fun <T> tweenShort() = tween<T>(DURATION_SHORT, easing = Standard)
    fun <T> tweenLong() = tween<T>(DURATION_LONG, easing = EmphasizedDecelerate)
}
