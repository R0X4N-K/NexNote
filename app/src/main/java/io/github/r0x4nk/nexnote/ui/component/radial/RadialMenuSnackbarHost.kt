package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A [SnackbarHost] wired into the [RadialMenuController] so the active FAB
 * automatically lifts above the snackbar — the standard Material 3
 * interaction popularised by Gmail, Tasks and Google Calendar.
 *
 * Rather than offsetting the snackbar *above* the FAB (which keeps the FAB
 * static and leaves a visual gap below the toast), this host renders the
 * snackbar at its natural bottom position and publishes the measured height
 * to [RadialMenuController.transientBottomObstructionPx]. The overlay folds
 * that height into the FAB's safe-bottom inset, so its existing spring
 * animation makes the FAB glide up and back down responsively.
 *
 * @param hostState           The [SnackbarHostState] driving message
 *                            presentation. Same semantics as the standard
 *                            Material 3 [SnackbarHost].
 * @param bottomInset         Bottom padding the host must keep clear (typically
 *                            the outer Scaffold's bottom-bar height). Without
 *                            this the snackbar would slide under the app's
 *                            bottom navigation bar.
 * @param modifier            Additional layout modifier — applied AFTER the
 *                            inset padding and size reporting.
 * @param snackbar            Renderer for an active snackbar; defaults to a
 *                            stock Material 3 [Snackbar]. Callers can supply
 *                            a custom slot to apply theming, but the default
 *                            keeps every screen visually consistent.
 */
@Composable
internal fun RadialMenuSnackbarHost(
    hostState: SnackbarHostState,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { data -> Snackbar(snackbarData = data) }
) {
    val controller = LocalRadialMenuController.current

    // Ensure the obstruction value cannot outlive this composable. If the
    // host leaves composition (e.g. screen navigation) while a snackbar was
    // visible, the FAB on the next screen would otherwise stay lifted.
    DisposableEffect(controller) {
        onDispose { controller.transientBottomObstructionPx = 0 }
    }

    // We wrap the SnackbarHost in an outer Box that owns the bottom inset, and
    // put `onSizeChanged` on the inner SnackbarHost directly. Doing it this
    // way removes any ambiguity about modifier ordering: the height we read is
    // exactly the snackbar's intrinsic height (zero while no snackbar is on
    // screen), independent of `bottomInset`. That value is what feeds the
    // FAB's "lift" animation; mixing in the inset would push the FAB further
    // up than it should go.
    Box(modifier = Modifier.padding(bottom = bottomInset).then(modifier)) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier.onSizeChanged { size ->
                // SnackbarHost reports `0` when no snackbar is visible (its
                // internal AnimatedVisibility collapses to zero height), and
                // the snackbar's measured height while one is on screen.
                // Mirroring that value into the controller is sufficient to
                // drive the FAB's lift/return spring animation.
                controller.transientBottomObstructionPx = size.height
            },
            snackbar = snackbar
        )
    }
}
