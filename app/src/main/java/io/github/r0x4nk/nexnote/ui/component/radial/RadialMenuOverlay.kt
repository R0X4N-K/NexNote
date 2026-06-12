package io.github.r0x4nk.nexnote.ui.component.radial

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

    /** Gap reserved above the FAB so adjacent UI never visually touches it. */
    private val fabContentGap = 16.dp

    /**
     * Vertical space (in dp) that scrollable content must keep free at the
     * bottom of the screen so the floating action button does not occlude the
     * last items in the list. The value stacks the outer chrome (bottom
     * navigation + system inset, expressed by [fabBottomOffset]) on top of the
     * FAB's own footprint (margin + size + a small breathing gap).
     *
     * Use this as the trailing padding/spacer of a [androidx.compose.foundation.lazy.LazyColumn]
     * or as the bottom inset of a [androidx.compose.foundation.layout.Box] whose
     * content extends to the bottom edge.
     *
     * Note: this value is intentionally NOT used to position the Material
     * snackbar anymore. With the Material 3 "lift the FAB on snackbar" pattern
     * (see [RadialMenuController.transientBottomObstructionPx]) the snackbar
     * sits at its natural bottom position and the FAB rises out of its way.
     */
    fun fabBottomClearance(fabBottomOffset: Dp): Dp =
        fabBottomOffset + fabMargin + fabSize + fabContentGap
}

private val BUTTON_SIZE_DP         = RadialMenuOverlayDefaults.fabSize
private val BUTTON_MARGIN_DP       = RadialMenuOverlayDefaults.fabMargin
private val SCROLL_SHORTCUT_RADIAL_RADIUS_OFFSET = 12.dp

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
 * On top of that bottom inset, any transient bottom-anchored UI (currently
 * the Material snackbar — see [RadialMenuController.transientBottomObstructionPx])
 * adds its height to the safe area, so the FAB lifts above a visible
 * snackbar in the same Material 3 fashion as Gmail / Tasks.
 *
 * Callers should pass `innerPadding.calculateBottomPadding()` from the outer
 * [androidx.compose.material3.Scaffold] so the FAB clears both the system nav
 * bar and any visible bottom navigation bar. When neither a keyboard nor a
 * bottom bar is present, the system nav bar alone is used.
 *
 * The FAB slides with the keyboard (and with snackbar appear/dismiss) via a
 * single spring animation so its motion matches the system keyboard curve and
 * snackbar transitions.
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
            //
            // On top of whichever wins, we stack `transientBottomObstructionPx`
            // (e.g. the live snackbar height) so the FAB rises above transient
            // bottom-anchored UI — the Material 3 "lift the FAB" interaction.
            // This addition is intentional (not max()): a snackbar is layered
            // above the bottom inset, not overlapping it.
            val imeBottomPx        = WindowInsets.ime.getBottom(density)
            val navBarBottomPx     = WindowInsets.navigationBars.getBottom(density)
            val fabBottomOffsetPx  = with(density) { fabBottomOffset.toPx() }
            val transientInsetPx   = controller.transientBottomObstructionPx.toFloat()
            val safeBottomPx       = maxOf(
                imeBottomPx.toFloat(),
                navBarBottomPx.toFloat(),
                fabBottomOffsetPx
            ) + transientInsetPx

            // Animate safe-bottom so the FAB glides smoothly with both the
            // keyboard and the snackbar appear/dismiss cycle (both feed into
            // `safeBottomPx`, so a single animation channel suffices).
            val animatedSafeBottomPx by animateFloatAsState(
                targetValue   = safeBottomPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label         = "fabBottomOffset"
            )

            val fabX = if (isLeftHanded) buttonMarginPx
                       else containerW - buttonSizePx - buttonMarginPx
            val fabY = containerH - buttonSizePx - buttonMarginPx - animatedSafeBottomPx

            val fabCenter = Offset(fabX + buttonSizePx / 2f, fabY + buttonSizePx / 2f)

            // Fixed safe arc: items always land above the navigation bar. The
            // angles stay stable across screens; editor scroll shortcuts only
            // add radial distance below.
            val arcStartDeg = if (isLeftHanded) ARC_START_LEFT else ARC_START_RIGHT
            val arcEndDeg   = if (isLeftHanded) ARC_END_LEFT   else ARC_END_RIGHT

            val hasMenuItems = controller.items.isNotEmpty()
            val directFabAction = controller.fabAction
            val scrollToTop    = controller.scrollToTopAction
            val scrollToBottom = controller.scrollToBottomAction
            val hasScrollShortcuts = scrollToTop != null && scrollToBottom != null
            val showScrollShortcuts = hasScrollShortcuts && !controller.overrideFabHidden
            val radiusOffset = if (hasScrollShortcuts) {
                SCROLL_SHORTCUT_RADIAL_RADIUS_OFFSET
            } else {
                0.dp
            }
            val showFab = (hasMenuItems || directFabAction != null) && !controller.overrideFabHidden

            // Auto-close the radial menu whenever the FAB becomes invisible
            // or stops owning radial items (e.g. switches to a direct action).
            LaunchedEffect(showFab, hasMenuItems) {
                if ((!showFab || !hasMenuItems) && menuState.isOpen) {
                    menuState = RadialMenuState()
                }
            }

            Box(Modifier.fillMaxSize()) {
                // ── App content ──────────────────────────────────────────────────
                content()

                // ── Radial menu (renders below the FAB in z-order) ───────────────
                if (menuState.isOpen && hasMenuItems) {
                    RadialMenu(
                        state       = menuState,
                        items       = controller.items,
                        radiusOffset = radiusOffset,
                        onItemClick = { index ->
                            val snapshot = controller.items.toList()
                            menuState = RadialMenuState()
                            if (index in snapshot.indices) snapshot[index].action()
                        },
                        onDismiss = { menuState = RadialMenuState() }
                    )
                }

                // ── Scroll shortcut buttons (editor-only, stacked above the FAB slot) ──
                // Shown only while an editor screen has registered scroll callbacks
                // and the screen has not explicitly suppressed floating controls.
                if (showScrollShortcuts) {
                    ScrollShortcutButtons(
                        fabX             = fabX,
                        fabY             = fabY,
                        buttonSizePx     = buttonSizePx,
                        alpha            = controller.scrollShortcutAlpha,
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
                        closedContentDescription = closedFabContentDescription(
                            hasDirectAction = directFabAction != null,
                            customDescription = controller.fabContentDescription
                        ),
                        onClick = {
                            val action = controller.fabAction
                            if (action != null) {
                                menuState = RadialMenuState()
                                action()
                            } else if (hasMenuItems) {
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
                        }
                    )
                }
            }
        }
    }
}

private fun closedFabContentDescription(
    hasDirectAction: Boolean,
    customDescription: String?
): String = customDescription ?: if (hasDirectAction) "Activate action" else "Open menu"
