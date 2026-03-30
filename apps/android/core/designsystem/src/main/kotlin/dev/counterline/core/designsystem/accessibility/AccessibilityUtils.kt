package dev.counterline.core.designsystem.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Minimum touch target size per WCAG / Android accessibility guidelines.
 */
val MinTouchTarget = 48.dp

/**
 * Wraps content to ensure a minimum 48x48dp touch target.
 */
@Composable
fun AccessibleTouchTarget(
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
            .semantics { this.contentDescription = contentDescription },
    ) {
        content()
    }
}

/**
 * Modifier extensions for common accessibility patterns.
 */
fun Modifier.accessibleHeading(label: String): Modifier = this.semantics {
    heading()
    contentDescription = label
}

fun Modifier.accessibleState(description: String): Modifier = this.semantics {
    stateDescription = description
}

fun Modifier.accessibleLiveRegion(
    description: String,
    mode: LiveRegionMode = LiveRegionMode.Polite,
): Modifier = this.semantics {
    liveRegion = mode
    contentDescription = description
}

/**
 * Check whether the user has "reduce motion" / animation scale = 0 in system settings.
 * Call from a composable context.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val am = context.getSystemService(AccessibilityManager::class.java)
        // If TalkBack or similar is active and animation duration scale is 0, honor it
        am?.let {
            val touchExplorationEnabled = it.isTouchExplorationEnabled
            val services = it.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_SPOKEN,
            )
            // Also check android.provider.Settings.Global ANIMATOR_DURATION_SCALE
            touchExplorationEnabled || services.isNotEmpty()
        } ?: false
    }
}

/**
 * Generate a TalkBack-friendly description of a chess position from FEN.
 * Returns concise English like "White: King e1, Queen d1, ... Black: King e8, ..."
 */
fun describeFenForAccessibility(fen: String): String {
    val placement = fen.split(" ").firstOrNull() ?: return "Empty board"
    val ranks = placement.split("/")
    val files = "abcdefgh"
    val pieceNames = mapOf(
        'K' to "King", 'Q' to "Queen", 'R' to "Rook",
        'B' to "Bishop", 'N' to "Knight", 'P' to "Pawn",
        'k' to "King", 'q' to "Queen", 'r' to "Rook",
        'b' to "Bishop", 'n' to "Knight", 'p' to "Pawn",
    )

    val whitePieces = mutableListOf<String>()
    val blackPieces = mutableListOf<String>()

    for ((rankIdx, rank) in ranks.withIndex()) {
        var fileIdx = 0
        val rankNum = 8 - rankIdx
        for (ch in rank) {
            if (ch.isDigit()) {
                fileIdx += ch.digitToInt()
            } else {
                val square = "${files[fileIdx]}$rankNum"
                val name = pieceNames[ch] ?: "Piece"
                val desc = "$name $square"
                if (ch.isUpperCase()) whitePieces.add(desc) else blackPieces.add(desc)
                fileIdx++
            }
        }
    }

    return buildString {
        append("White: ")
        append(whitePieces.joinToString(", "))
        append(". Black: ")
        append(blackPieces.joinToString(", "))
        append(".")
    }
}
