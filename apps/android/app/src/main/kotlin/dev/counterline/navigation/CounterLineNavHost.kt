package dev.counterline.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.counterline.feature.deviations.DeviationsScreen
import dev.counterline.feature.drill.DrillScreen
import dev.counterline.feature.exam.ExamScreen
import dev.counterline.feature.home.HomeScreen
import dev.counterline.feature.modelgames.ModelGamesScreen
import dev.counterline.feature.plans.PlansScreen
import dev.counterline.feature.progress.ProgressScreen
import dev.counterline.feature.repertoire.RepertoireScreen
import dev.counterline.feature.settings.SettingsScreen

enum class TopLevelRoute(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Default.Home),
    REPERTOIRE("repertoire", "Lines", Icons.Default.LibraryBooks),
    DRILL("drill", "Drill", Icons.Default.FitnessCenter),
    PROGRESS("progress", "Progress", Icons.Default.QueryStats),
    SETTINGS("settings", "Settings", Icons.Default.Settings),
}

object NestedRoutes {
    const val PLANS = "plans"
    const val DEVIATIONS = "deviations"
    const val MODEL_GAMES = "model_games"
    const val EXAM = "exam"
}

@Composable
fun CounterLineApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = TopLevelRoute.entries.toList()
    val showBottomBar = currentDestination?.route in topLevelRoutes.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelRoutes.forEach { dest ->
                        NavigationBarItem(
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelRoute.HOME.route) {
                HomeScreen(
                    onNavigateToRepertoire = { navController.navigate(TopLevelRoute.REPERTOIRE.route) },
                    onNavigateToDrill = { navController.navigate(TopLevelRoute.DRILL.route) },
                    onNavigateToPlans = { navController.navigate(NestedRoutes.PLANS) },
                    onNavigateToDeviations = { navController.navigate(NestedRoutes.DEVIATIONS) },
                    onNavigateToModelGames = { navController.navigate(NestedRoutes.MODEL_GAMES) },
                    onNavigateToExam = { navController.navigate(NestedRoutes.EXAM) },
                )
            }
            composable(TopLevelRoute.REPERTOIRE.route) {
                RepertoireScreen()
            }
            composable(TopLevelRoute.DRILL.route) {
                DrillScreen()
            }
            composable(TopLevelRoute.PROGRESS.route) {
                ProgressScreen()
            }
            composable(TopLevelRoute.SETTINGS.route) {
                SettingsScreen()
            }
            composable(NestedRoutes.PLANS) {
                PlansScreen(onBack = { navController.popBackStack() })
            }
            composable(NestedRoutes.DEVIATIONS) {
                DeviationsScreen(onBack = { navController.popBackStack() })
            }
            composable(NestedRoutes.MODEL_GAMES) {
                ModelGamesScreen(onBack = { navController.popBackStack() })
            }
            composable(NestedRoutes.EXAM) {
                ExamScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
