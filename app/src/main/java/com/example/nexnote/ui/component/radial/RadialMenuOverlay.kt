package com.example.nexnote.ui.component.radial

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object RadialMenuOverlayDefaults {
    val fabSize = 56.dp
    val fabMargin = 24.dp

    private val snackbarGap = 16.dp

    fun snackbarBottomPadding(fabBottomOffset: Dp): Dp =
        fabBottomOffset + fabMargin + fabSize + snackbarGap
}

private val BUTTON_SIZE_DP         = RadialMenuOverlayDefaults.fabSize
private val BUTTON_MARGIN_DP       = RadialMenuOverlayDefaults.fabMargin

// Fixed arc ranges keep all items safely above the navigation bar.
//
// Angles use the clockwise-from-12-o'clock convention:
//   0° = directly above, 90° = right, 180° = below, 270° = left.
//
// Right-handed: FAB at bottom-right, items fan from 285° to 360°
//   (lower-left to directly above FAB).  At 285° the item is ~36 dp above the
//   FAB centre; labels never descend into the navigation-bar zone.
//
// Left-handed: FAB at bottom-left, arc spans 0° to 75° (directly above to
//   upper-right). Geometry is symmetric.
//
// The previous variable-spread approach could place items at 252°–270° where
// sin(mathAngle) ≥ 0, meaning items sat at or below the FAB centre and their
// labels overlapped the navigation bar on small screens. The fixed range
// eliminates this entirely.
private const val ARC_START_RIGHT = 285f
private const val ARC_END_RIGHT   = 360f
private const val ARC_START_LEFT  =   0f
private const val ARC_END_LEFT    =  75f

/**
 * Full-screen wrapper that owns the floating action button (FAB) and the
 * radial context menu.
 *
 * **FAB positioning**
 *
 * The FAB bottom edge is kept above the tallest of:
 *   - the system IME (soft keyboard),
 *   - the system navigation bar,
 *   - [fabBottomOffset] — the app's own bottom chrome (bottom-tab bar).
 *
 * Callers should pass `innerPadding.calculateBottomPadding()` from the outer
 * [androidx.compose.material3.Scaffold] so the FAB clears both the system nav
 * bar and any visible bottom navigation bar. When neither a keyboard nor a
 * bottom bar is present, the system nav bar alone is used.
 *
 * The FAB slides with the keyboard via a spring animation so its motion
 * matches the system keyboard curve.
 *
 * **Visibility**
 *
 * The FAB is hidden when:
 *   - no screen has registered any menu items ([RadialMenuController.items] is
 *     empty), or
 *   - a screen has explicitly suppressed it ([RadialMenuController.overrideFabHidden]
 *     is true — e.g. the editor while the keyboard is open).
 *
 * When the FAB becomes hidden while the menu is open, the menu is closed
 * automatically.
 *
 * **FAB icon**
 *
 * The closed-state FAB icon defaults to Add but can be overridden per screen
 * via [RadialMenuController.fabIcon]. The editor uses a tools icon to
 * communicate that the menu contains editor-specific actions.
 *
 * **Handedness**
 *
 * [isLeftHanded] moves the FAB to the bottom-left corner and mirrors the
 * radial arc so items always expand away from the screen corner.
 */
@Composable
fun RadialMenuOverlay(
    isLeftHanded: Boolean = false,
    fabBottomOffset: Dp = 0.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val controller = remember { RadialMenuController() }
    var menuState by remember { mutableStateOf(RadialMenuState()) }

    CompositionLocalProvider(LocalRadialMenuController provides controller) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val density           = LocalDensity.current
            val buttonSizePx      = with(density) { BUTTON_SIZE_DP.toPx() }
            val buttonMarginPx    = with(density) { BUTTON_MARGIN_DP.toPx() }
            val containerW        = with(density) { maxWidth.toPx() }
            val containerH        = with(density) { maxHeight.toPx() }

            // Bottom clearance is the largest of three competing values:
            //  • IME height      — keyboard is open
            //  • system nav bar  — keyboard is closed, no bottom bar
            //  • fabBottomOffset — Scaffold's bottom bar (bottom tabs + nav bar)
            // Using max() prevents double-counting: when the bottom bar is
            // shown, fabBottomOffset already includes the system nav bar.
            val imeBottomPx       = WindowInsets.ime.getBottom(density)
            val navBarBottomPx    = WindowInsets.navigationBars.getBottom(density)
            val fabBottomOffsetPx = with(density) { fabBottomOffset.toPx() }
            val safeBottomPx      = maxOf(
                imeBottomPx.toFloat(),
                navBarBottomPx.toFloat(),
                fabBottomOffsetPx
            )

            // Animate safe-bottom so the FAB glides smoothly with the keyboard.
            val animatedSafeBottomPx by animateFloatAsState(
                targetValue   = safeBottomPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label         = "fabKeyboardOffset"
            )

            val fabX = if (isLeftHanded) buttonMarginPx
                       else containerW - buttonSizePx - buttonMarginPx
            val fabY = containerH - buttonSizePx - buttonMarginPx - animatedSafeBottomPx

            val fabCenter = Offset(fabX + buttonSizePx / 2f, fabY + buttonSizePx / 2f)

            // Fixed safe arc: items always land above the navigation bar.
            val arcStartDeg = if (isLeftHanded) ARC_START_LEFT else ARC_START_RIGHT
            val arcEndDeg   = if (isLeftHanded) ARC_END_LEFT   else ARC_END_RIGHT

            val showFab = controller.items.isNotEmpty() && !controller.overrideFabHidden

            // Auto-close the radial menu whenever the FAB becomes invisible
            // (e.g. the editor's keyboard opens while the menu is open).
            LaunchedEffect(showFab) {
                if (!showFab && menuState.isOpen) menuState = RadialMenuState()
            }

            Box(Modifier.fillMaxSize()) {
                // ── App content ──────────────────────────────────────────────────
                content()

                // ── Radial menu (renders below the FAB in z-order) ───────────────
                if (menuState.isOpen) {
                    RadialMenu(
                        state       = menuState,
                        items       = controller.items,
                        onItemClick = { index ->
                            val snapshot = controller.items.toList()
                            menuState = RadialMenuState()
                            if (index in snapshot.indices) snapshot[index].action()
                        },
                        onDismiss = { menuState = RadialMenuState() }
                    )
                }

                // ── Scroll shortcut buttons (editor-only, stacked above the FAB) ──
                // Shown only while an editor screen has registered scroll callbacks
                // and the FAB itself is visible (same visibility guard).
                val scrollToTop    = controller.scrollToTopAction
                val scrollToBottom = controller.scrollToBottomAction
                if (showFab && scrollToTop != null && scrollToBottom != null) {
                    ScrollShortcutButtons(
                        fabX             = fabX,
                        fabY             = fabY,
                        buttonSizePx     = buttonSizePx,
                        onScrollToTop    = scrollToTop,
                        onScrollToBottom = scrollToBottom
                    )
                }

                // ── FAB (always on top) ──────────────────────────────────────────
                if (showFab) {
                    StaticMenuButton(
                        isMenuOpen   = menuState.isOpen,
                        fabX         = fabX,
                        fabY         = fabY,
                        buttonSizePx = buttonSizePx,
                        closedIcon   = controller.fabIcon ?: Icons.Default.Add,
                        onToggle     = {
                            menuState = if (menuState.isOpen) {
                                RadialMenuState()
                            } else {
                                RadialMenuState(
                                    isOpen        = true,
                                    center        = fabCenter,
                                    selectedIndex = -1,
                                    arcStartDeg   = arcStartDeg,
                                    arcEndDeg     = arcEndDeg
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
