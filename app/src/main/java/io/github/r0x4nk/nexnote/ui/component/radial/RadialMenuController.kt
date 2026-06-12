package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    private var activeRegistration: Any? = null

    var items by mutableStateOf<List<RadialMenuItem>>(emptyList())
        internal set

    var fabIcon by mutableStateOf<ImageVector?>(null)
        internal set

    var fabContentDescription by mutableStateOf<String?>(null)
        internal set

    var fabAction by mutableStateOf<(() -> Unit)?>(null)
        internal set

    var overrideFabHidden by mutableStateOf(false)
        internal set

    /** Non-null only while the editor is active. Scrolls the note content to the top. */
    var scrollToTopAction by mutableStateOf<(() -> Unit)?>(null)
        internal set

    /** Non-null only while the editor is active. Scrolls the note content to the bottom. */
    var scrollToBottomAction by mutableStateOf<(() -> Unit)?>(null)
        internal set

    /** Editor-owned opacity for scroll shortcuts when they would sit near the cursor. */
    var scrollShortcutAlpha by mutableFloatStateOf(1f)
        internal set

    /**
     * Pixel height of a transient bottom-anchored UI element (e.g. a Material
     * snackbar) that should push the FAB upward — matching the Material 3
     * "snackbar lifts the FAB" interaction used by Gmail, Tasks, etc.
     *
     * Screens publish a height here via [RadialMenuSnackbarHost] whenever
     * their snackbar measures itself. The [RadialMenuOverlay] folds this
     * value into the FAB's bottom clearance, so the FAB rises through its
     * existing spring animation rather than being overlapped by the snackbar.
     *
     * `0` means no obstruction (or the snackbar is fully dismissed).
     */
    var transientBottomObstructionPx by mutableIntStateOf(0)
        internal set

    internal fun register(
        owner: Any,
        items: List<RadialMenuItem>,
        fabIcon: ImageVector?,
        fabContentDescription: String?,
        fabAction: (() -> Unit)?
    ) {
        activeRegistration = owner
        this.items = items
        this.fabIcon = fabIcon
        this.fabContentDescription = fabContentDescription
        this.fabAction = fabAction
    }

    internal fun clearRegistration(owner: Any) {
        if (activeRegistration !== owner) return
        activeRegistration = null
        items = emptyList()
        fabIcon = null
        fabContentDescription = null
        fabAction = null
    }
}

val LocalRadialMenuController = compositionLocalOf { RadialMenuController() }

/**
 * Called from each screen to register its radial menu items and optional
 * closed-state FAB metadata for the current destination.
 *
 * Items are only applied when the owning back-stack entry is **RESUMED**.
 * During a predictive-back gesture the destination screen is composed at
 * lifecycle state STARTED (not RESUMED), so this guard prevents the FAB from
 * appearing in the preview frame before the transition is committed.
 *
 * Items (and FAB metadata) are cleared automatically in
 * [DisposableEffect.onDispose] when the screen leaves composition, but only if
 * the same registration still owns the controller. That owner-token check
 * prevents the navigation race where the new screen sets its FAB state and then
 * the old screen's dispose clears everything.
 *
 * @param items Items to show in the radial arc.
 * @param fabIcon Closed-state icon for the FAB. Null falls back to Add.
 * @param fabContentDescription Accessibility label for the closed FAB.
 */
@Composable
fun RadialMenuEffect(
    items: List<RadialMenuItem>,
    fabIcon: ImageVector? = null,
    fabContentDescription: String? = null
) {
    RadialFabRegistrationEffect(
        items = items,
        fabIcon = fabIcon,
        fabContentDescription = fabContentDescription,
        directFabAction = null
    )
}

/**
 * Registers the shared FAB as a single direct action for screens that do not
 * need a radial arc. This keeps one-item menus from adding an unnecessary
 * second tap while preserving the same placement, insets, and snackbar lift
 * behaviour as [RadialMenuEffect].
 */
@Composable
fun RadialFabActionEffect(
    contentDescription: String,
    onClick: () -> Unit,
    fabIcon: ImageVector? = null
) {
    RadialFabRegistrationEffect(
        items = emptyList(),
        fabIcon = fabIcon,
        fabContentDescription = contentDescription,
        directFabAction = onClick
    )
}

@Composable
private fun RadialFabRegistrationEffect(
    items: List<RadialMenuItem>,
    fabIcon: ImageVector?,
    fabContentDescription: String?,
    directFabAction: (() -> Unit)?
) {
    val controller     = LocalRadialMenuController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val registration   = remember { Any() }
    val currentFabAction by rememberUpdatedState(directFabAction)
    val hasDirectFabAction = directFabAction != null
    val registeredFabAction: (() -> Unit)? = remember(hasDirectFabAction) {
        if (hasDirectFabAction) {
            { currentFabAction?.invoke() }
        } else {
            null
        }
    }

    // Observe lifecycle state reactively so DisposableEffect re-runs on every
    // STARTED ↔ RESUMED transition.
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    DisposableEffect(items, fabIcon, fabContentDescription, registeredFabAction, isResumed) {
        if (isResumed) {
            controller.register(
                owner = registration,
                items = items,
                fabIcon = fabIcon,
                fabContentDescription = fabContentDescription,
                fabAction = registeredFabAction
            )
        } else {
            // Screen is in back-preview (STARTED): remove its items so the FAB
            // does not flash before the gesture is committed.
            controller.clearRegistration(registration)
        }
        onDispose {
            controller.clearRegistration(registration)
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
    onScrollToBottom: () -> Unit,
    shortcutAlpha: Float = 1f
) {
    val controller    = LocalRadialMenuController.current
    val currentTop    by rememberUpdatedState(onScrollToTop)
    val currentBottom by rememberUpdatedState(onScrollToBottom)

    SideEffect {
        controller.scrollShortcutAlpha = shortcutAlpha.coerceIn(0f, 1f)
    }

    DisposableEffect(controller) {
        val topAction = { currentTop() }
        val bottomAction = { currentBottom() }

        controller.scrollToTopAction    = topAction
        controller.scrollToBottomAction = bottomAction
        onDispose {
            val ownsTopAction = controller.scrollToTopAction === topAction
            val ownsBottomAction = controller.scrollToBottomAction === bottomAction
            if (ownsTopAction) {
                controller.scrollToTopAction = null
            }
            if (ownsBottomAction) {
                controller.scrollToBottomAction = null
            }
            if (ownsTopAction || ownsBottomAction) {
                controller.scrollShortcutAlpha = 1f
            }
        }
    }
}
