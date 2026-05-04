package com.example.nexnote.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nexnote.ui.component.radial.RadialMenuOverlay

/**
 * Root navigation graph.
 *
 * The bottom bar is shown only on primary destinations (Home, Agenda, Tags,
 * Templates, Settings). Editor and Trash are secondary destinations with no
 * bottom bar.
 *
 * The outer Scaffold owns only the bottom bar; contentWindowInsets = (0,0,0,0)
 * prevents it from consuming status-bar insets, which are handled by each
 * screen's own Scaffold.
 *
 * [RadialMenuOverlay] is placed INSIDE the Scaffold's content lambda so that
 * fabBottomOffset is the correct measured value on the very first composition.
 * The padding is intentionally used only to lift floating controls; the screen
 * content itself remains full height and can draw behind the floating bottom
 * bar.
 *
 * [isLeftHanded] mirrors the FAB to the bottom-left corner and adjusts the
 * radial arc direction so items always stay fully on-screen.
 */
@Composable
fun AppNavigation(isLeftHanded: Boolean = false) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        modifier            = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AppBottomBar(
                showBottomBar = showBottomBar,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    ) { innerPadding ->
        AppNavigationContent(
            navController = navController,
            innerPadding = innerPadding,
            isLeftHanded = isLeftHanded
        )
    }
}

@Composable
private fun AppNavigationContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    isLeftHanded: Boolean
) {
    // RadialMenuOverlay lives inside the Scaffold content lambda so
    // fabBottomOffset is based on the already measured innerPadding.
    RadialMenuOverlay(
        isLeftHanded    = isLeftHanded,
        fabBottomOffset = innerPadding.calculateBottomPadding()
    ) {
        AppNavHost(
            navController = navController,
            floatingBottomPadding = innerPadding.calculateBottomPadding()
        )
    }
}

@Composable
private fun AppBottomBar(
    showBottomBar: Boolean,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    if (showBottomBar) {
        FloatingBottomBar {
            bottomNavItems.forEach { item ->
                AppBottomNavItem(
                    item = item,
                    currentDestination = currentDestination,
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FloatingBottomBar(content: @Composable RowScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
            )
        ) {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
private fun RowScope.AppBottomNavItem(
    item: BottomNavItem,
    currentDestination: NavDestination?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavigationBarItem(
        modifier = modifier,
        selected = item.isSelected(currentDestination),
        onClick = { navController.navigateBottomNav(item.screen.route) },
        icon = { Icon(item.icon, contentDescription = item.label) },
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

private fun BottomNavItem.isSelected(currentDestination: NavDestination?): Boolean =
    currentDestination?.hierarchy?.any { it.route == screen.route } == true

private fun NavHostController.navigateBottomNav(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState    = true
    }
}

private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Agenda.route,
    Screen.Tags.route,
    Screen.Templates.route,
    Screen.Settings.route
)

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Notes",     Icons.AutoMirrored.Filled.Note),
    BottomNavItem(Screen.Agenda,    "Agenda",    Icons.Default.CalendarToday),
    BottomNavItem(Screen.Tags,      "Tags",      Icons.Default.Tag),
    BottomNavItem(Screen.Templates, "Templates", Icons.Default.Description),
    BottomNavItem(Screen.Settings,  "Settings",  Icons.Default.Settings),
)
