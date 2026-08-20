package io.github.r0x4nk.nexnote.ui.navigation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.fileimport.ExternalFileOpenRequest
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlay

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
fun AppNavigation(
    isLeftHanded: Boolean = false,
    protectVaultRecentPreviews: Boolean = true,
    lockVaultOnBackground: Boolean = true,
    vaultAutoLockTimeout: VaultAutoLockTimeout = VaultAutoLockTimeout.IMMEDIATELY,
    vaultState: VaultState = VaultState.NOT_CONFIGURED,
    externalFileOpenRequest: ExternalFileOpenRequest? = null,
    onExternalFileOpenConsumed: (Long) -> Unit = {},
    onVaultAutoLockRequested: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavRoutes
    val vaultNoteId = backStackEntry?.arguments?.getLong(Screen.Editor.ARG_VAULT_NOTE_ID)
    val protectWindow = shouldProtectVaultRecentPreviews(
        protectVaultRecentPreviews = protectVaultRecentPreviews,
        route = currentDestination?.route,
        vaultNoteId = vaultNoteId
    )

    VaultRecentPreviewProtectionEffect(protectWindow)
    VaultAutoLockOnStopEffect(
        vaultState = vaultState,
        lockImmediatelyOnBackground = lockVaultOnBackground,
        onLockVault = onVaultAutoLockRequested
    )
    VaultAutoLockOnScreenOffEffect(
        vaultState = vaultState,
        lockImmediatelyOnBackground = lockVaultOnBackground,
        onLockVault = onVaultAutoLockRequested
    )
    VaultAutoLockOnResumeEffect(
        vaultState = vaultState,
        vaultAutoLockTimeout = vaultAutoLockTimeout,
        onLockVault = onVaultAutoLockRequested
    )
    ExternalFileOpenEffect(
        navController = navController,
        isNavigationReady = currentDestination != null,
        request = externalFileOpenRequest,
        onConsumed = onExternalFileOpenConsumed
    )

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
private fun ExternalFileOpenEffect(
    navController: NavHostController,
    isNavigationReady: Boolean,
    request: ExternalFileOpenRequest?,
    onConsumed: (Long) -> Unit
) {
    LaunchedEffect(request?.requestId, isNavigationReady) {
        if (!isNavigationReady) return@LaunchedEffect
        val openRequest = request ?: return@LaunchedEffect
        navController.navigate(Screen.Editor.existingNoteRoute(openRequest.noteId)) {
            launchSingleTop = true
        }
        onConsumed(openRequest.requestId)
    }
}

@Composable
private fun VaultAutoLockOnScreenOffEffect(
    vaultState: VaultState,
    lockImmediatelyOnBackground: Boolean,
    onLockVault: () -> Unit
) {
    val context = LocalContext.current
    val currentVaultState by rememberUpdatedState(vaultState)
    val currentLockImmediatelyOnBackground by rememberUpdatedState(lockImmediatelyOnBackground)
    val currentOnLockVault by rememberUpdatedState(onLockVault)

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF &&
                    shouldAutoLockVaultOnScreenOff(
                        lockImmediatelyOnBackground = currentLockImmediatelyOnBackground,
                        vaultState = currentVaultState
                    )
                ) {
                    currentOnLockVault()
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

@Composable
private fun VaultAutoLockOnStopEffect(
    vaultState: VaultState,
    lockImmediatelyOnBackground: Boolean,
    onLockVault: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity
    val currentVaultState by rememberUpdatedState(vaultState)
    val currentLockImmediatelyOnBackground by rememberUpdatedState(lockImmediatelyOnBackground)
    val currentOnLockVault by rememberUpdatedState(onLockVault)

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                shouldAutoLockVaultOnStop(
                    lockImmediatelyOnBackground = currentLockImmediatelyOnBackground,
                    vaultState = currentVaultState,
                    isChangingConfigurations = activity?.isChangingConfigurations == true
                )
            ) {
                currentOnLockVault()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Auto-lock the Vault on app resume based on [VaultAutoLockTimeout].
 *
 * Records a monotonic background timestamp on [Lifecycle.Event.ON_STOP] regardless of
 * the immediate background lock preference, then evaluates [shouldAutoLockVaultOnResume]
 * on [Lifecycle.Event.ON_START] and invokes [onLockVault] when the policy requires it.
 *
 * The timestamp lives only in memory at this composable scope: it is not persisted to
 * DataStore and is intentionally reset when the activity is destroyed and recreated.
 * Configuration changes (e.g. rotation) skip the recording to avoid spurious resume
 * locks right after recreation. Vault contents, PIN, keys and decrypted state are
 * never read by this effect.
 */
@Composable
private fun VaultAutoLockOnResumeEffect(
    vaultState: VaultState,
    vaultAutoLockTimeout: VaultAutoLockTimeout,
    onLockVault: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity
    val currentVaultState by rememberUpdatedState(vaultState)
    val currentVaultAutoLockTimeout by rememberUpdatedState(vaultAutoLockTimeout)
    val currentOnLockVault by rememberUpdatedState(onLockVault)
    val backgroundTimestampHolder = remember { LongArray(1) { Long.MIN_VALUE } }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (activity?.isChangingConfigurations != true) {
                        backgroundTimestampHolder[0] = SystemClock.elapsedRealtime()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    val backgroundAt = backgroundTimestampHolder[0]
                    if (backgroundAt != Long.MIN_VALUE) {
                        val elapsed = SystemClock.elapsedRealtime() - backgroundAt
                        backgroundTimestampHolder[0] = Long.MIN_VALUE
                        if (shouldAutoLockVaultOnResume(
                                timeout = currentVaultAutoLockTimeout,
                                vaultState = currentVaultState,
                                elapsedSinceBackgroundMillis = elapsed
                            )
                        ) {
                            currentOnLockVault()
                        }
                    }
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun VaultRecentPreviewProtectionEffect(protectWindow: Boolean) {
    val window = (LocalContext.current as? Activity)?.window
    SideEffect {
        window?.setSecurePreviewProtection(protectWindow)
    }
    DisposableEffect(window) {
        onDispose {
            window?.setSecurePreviewProtection(false)
        }
    }
}

private fun Window.setSecurePreviewProtection(enabled: Boolean) {
    if (enabled) {
        addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
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
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
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
