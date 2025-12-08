package com.localllm.app.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Sealed class representing the bottom navigation destinations
 */
sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavScreen(
        route = "home",
        title = "Home",
        icon = Icons.Filled.Home
    )
    
    data object NewChat : BottomNavScreen(
        route = "new_chat",
        title = "New Chat",
        icon = Icons.Filled.AddComment
    )
    
    data object History : BottomNavScreen(
        route = "history",
        title = "History",
        icon = Icons.Filled.History
    )
    
    data object ModelLibrary : BottomNavScreen(
        route = "library",
        title = "Models",
        icon = Icons.Filled.CloudDownload
    )
}

/**
 * Main Bottom Navigation Bar for LocalLLM
 * Implements a four-tab navigation with dark theme and cyan accent
 */
@Composable
fun LocalLLMBottomNavBar(navController: NavHostController) {
    val screens = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.NewChat,
        BottomNavScreen.History,
        BottomNavScreen.ModelLibrary
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar(
        containerColor = Color(0xFF1E1E1E), // Deep dark grey/near-black
        contentColor = Color.Gray
    ) {
        screens.forEach { screen ->
            AddItem(
                screen = screen,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    }
}

/**
 * Helper function to add a navigation item to the bottom bar
 */
@Composable
private fun RowScope.AddItem(
    screen: BottomNavScreen,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
    
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title
            )
        },
        label = {
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelSmall
            )
        },
        selected = selected,
        onClick = {
            navController.navigate(screen.route) {
                // Pop up to the start destination of the graph to
                // avoid building up a large stack of destinations
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination when
                // reselecting the same item
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF000000), // Dark/Black when active
            selectedTextColor = Color(0xFF00C8C8), // Bright cyan text when active
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray,
            indicatorColor = Color(0xFF00C8C8).copy(alpha = 0.15f) // Cyan indicator background
        )
    )
}
