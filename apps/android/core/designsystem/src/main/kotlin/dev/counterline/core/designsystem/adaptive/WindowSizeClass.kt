package dev.counterline.core.designsystem.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size classification aligned with Material 3 adaptive guidance.
 * - Compact: phones in portrait (< 600dp)
 * - Medium: large phones landscape, small tablets (600–839dp)
 * - Expanded: tablets, foldables unfolded (≥ 840dp)
 */
@Immutable
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Immutable
enum class WindowHeightClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Immutable
data class WindowSizeClass(
    val width: WindowWidthClass,
    val height: WindowHeightClass,
)

/**
 * Compute the current window size class from LocalConfiguration.
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp.dp
    val heightDp = config.screenHeightDp.dp

    return remember(widthDp, heightDp) {
        WindowSizeClass(
            width = when {
                widthDp < 600.dp -> WindowWidthClass.COMPACT
                widthDp < 840.dp -> WindowWidthClass.MEDIUM
                else -> WindowWidthClass.EXPANDED
            },
            height = when {
                heightDp < 480.dp -> WindowHeightClass.COMPACT
                heightDp < 900.dp -> WindowHeightClass.MEDIUM
                else -> WindowHeightClass.EXPANDED
            },
        )
    }
}

/**
 * Adaptive list-detail layout.
 * - On compact screens: shows either list or detail based on [showDetail].
 * - On medium/expanded: shows both panes side by side.
 */
@Composable
fun ListDetailLayout(
    showDetail: Boolean,
    listContent: @Composable (Modifier) -> Unit,
    detailContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    listWeight: Float = 0.4f,
) {
    val windowSize = rememberWindowSizeClass()

    when (windowSize.width) {
        WindowWidthClass.COMPACT -> {
            if (showDetail) {
                detailContent(modifier.fillMaxSize())
            } else {
                listContent(modifier.fillMaxSize())
            }
        }
        WindowWidthClass.MEDIUM, WindowWidthClass.EXPANDED -> {
            Row(modifier = modifier.fillMaxSize()) {
                listContent(Modifier.weight(listWeight).fillMaxHeight())
                VerticalDivider()
                detailContent(Modifier.weight(1f - listWeight).fillMaxHeight())
            }
        }
    }
}

/**
 * Adaptive board layout for drill/practice screens.
 * - Portrait: board on top, controls below
 * - Landscape / tablet: board left, controls right (side by side)
 */
@Composable
fun BoardControlLayout(
    board: @Composable (Modifier) -> Unit,
    controls: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSize = rememberWindowSizeClass()
    val useSideBySide = windowSize.height == WindowHeightClass.COMPACT ||
        windowSize.width != WindowWidthClass.COMPACT

    if (useSideBySide) {
        Row(modifier = modifier.fillMaxSize()) {
            board(Modifier.weight(0.5f).fillMaxHeight())
            controls(Modifier.weight(0.5f).fillMaxHeight())
        }
    } else {
        androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize()) {
            board(
                Modifier.fillMaxWidth().aspectRatio(1f),
            )
            controls(Modifier.weight(1f).fillMaxSize())
        }
    }
}
