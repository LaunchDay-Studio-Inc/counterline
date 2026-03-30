package dev.counterline.core.designsystem.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Adaptive navigation shell.
 * - Compact: bottom NavigationBar
 * - Medium+: side NavigationRail
 */
@Composable
fun AdaptiveNavigationLayout(
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val windowSize = rememberWindowSizeClass()

    when (windowSize.width) {
        WindowWidthClass.COMPACT -> {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        items.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = item.selected,
                                onClick = item.onClick,
                            )
                        }
                    }
                },
                modifier = modifier,
            ) { innerPadding ->
                content(Modifier.padding(innerPadding))
            }
        }
        WindowWidthClass.MEDIUM, WindowWidthClass.EXPANDED -> {
            Row(modifier = modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    items.forEach { item ->
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = item.selected,
                            onClick = item.onClick,
                        )
                    }
                }
                content(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
