package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Shared controller between [RadialMenuOverlay] (which reads the state) and
 * individual screens (which register items via [RadialMenuEffect]).
 *
 * [fabIcon] overrides the closed-state FAB icon. When null the default Add icon
 * is used. The editor sets this to a tools icon to communicate that the FAB
 * opens editor-specific actions rather than a generic "add" flow.
 *
 * [overrideFabHidden] lets a screen suppress the FAB without unregistering its
 * items — used by the editor to hide the FAB while the keyboard is open.
 */
class RadialMenuController {
    var items by mutableStateOf<List<RadialMenuItem>>(emptyList())
        internal set

    var fabIcon by mutableStateOf<ImageVector?>(null)
        internal set

    var overrideFabHidden by mutableStateOf(false)
        internal set

    /** Non-null only while the editor is active. Scrolls the note content to the top. */
    var scrollToTopAction by mutableStateOf<(() -> Unit)?>(null)
        internal set

    /** Non-null only while the editor is active. Scrolls the note content to the bottom. */
    var scrollToBottomAction by mutableStateOf<(() -> Unit)?>(null)
        internal set
}

val LocalRadialMenuController = compositionLocalOf { RadialMenuController() }

/**
 * Called from each screen to register its radial menu items and an optional
 * closed-state FAB icon for the current destination.
 *
 * Items are only applied when the owning back-stack entry is **RESUMED**.
 * During a predictive-back gesture the destination screen is composed at
 * lifecycle state STARTED (not RESUMED), so this guard prevents the FAB from
 * appearing in the preview frame before the transition is committed.
 *
 * Items (and the FAB icon) are cleared automatically in
 * [DisposableEffect.onDispose] when the screen leaves composition, but only if
 * they have not already been replaced by the incoming screen — the referential-
 * equality check on [items] prevents the navigation race where the new screen
 * sets its items and then the old screen's dispose clears everything.
 *
 * @param items   Items to show in the radial arc.
 * @param fabIcon Closed-state icon for the FAB. Null falls back to Add.
 */
@Composable
fun RadialMenuEffect(items: List<RadialMenuItem>, fabIcon: ImageVector? = null) {
    val controller     = LocalRadialMenuController.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle state reactively so DisposableEffect re-runs on every
    // STARTED ↔ RESUMED transition.
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    DisposableEffect(items, fabIcon, isResumed) {
        if (isResumed) {
            controller.items   = items
            controller.fabIcon = fabIcon
        } else {
            // Screen is in back-preview (STARTED): remove its items so the FAB
            // does not flash before the gesture is committed.
            if (controller.items === items) {
                controller.items   = emptyList()
                controller.fabIcon = null
            }
        }
        onDispose {
            if (controller.items === items) {
                controller.items   = emptyList()
                controller.fabIcon = null
            }
        }
    }
}

/**
 * Suppresses the FAB while [hide] is true, without unregistering the screen's
 * menu items. The FAB reappears automatically when [hide] becomes false.
 *
 * The effect cleans up on disposal so the flag is always reset to false when
 * this screen is no longer active.
 *
 * Typical use: the editor calls this while the soft keyboard is open to
 * prevent the FAB from covering the text being typed.
 */
@Composable
fun RadialMenuFabHideEffect(hide: Boolean) {
    val controller = LocalRadialMenuController.current
    DisposableEffect(hide) {
        controller.overrideFabHidden = hide
        onDispose { controller.overrideFabHidden = false }
    }
}

/**
 * Registers scroll-to-top and scroll-to-bottom callbacks with the
 * [RadialMenuController] for the duration of the calling screen's composition.
 *
 * [RadialMenuOverlay] reads these callbacks to render two small floating
 * buttons above the FAB. The buttons are only shown while this effect is
 * active (i.e. only in the editor) and share the same visibility state as
 * the FAB itself.
 *
 * [rememberUpdatedState] ensures the lambdas stored in the controller always
 * delegate to the latest callback instances, even if the calling site
 * recomposes with new lambda references.
 *
 * The disposal guard mirrors [RadialMenuEffect]: when one editor navigates to
 * another editor, the outgoing destination may dispose after the incoming one
 * has already registered callbacks. Referential checks keep the old destination
 * from clearing the new scroll shortcuts.
 */
@Composable
fun RadialMenuScrollEffect(
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit
) {
    val controller    = LocalRadialMenuController.current
    val currentTop    by rememberUpdatedState(onScrollToTop)
    val currentBottom by rememberUpdatedState(onScrollToBottom)

    DisposableEffect(controller) {
        val topAction = { currentTop() }
        val bottomAction = { currentBottom() }

        controller.scrollToTopAction    = topAction
        controller.scrollToBottomAction = bottomAction
        onDispose {
            if (controller.scrollToTopAction === topAction) {
                controller.scrollToTopAction = null
            }
            if (controller.scrollToBottomAction === bottomAction) {
                controller.scrollToBottomAction = null
            }
        }
    }
}
