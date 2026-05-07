package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Horizontal swipe gesture used in the editor content area to flip between the
 * edit and preview modes. The behaviour is driven by both a distance threshold
 * and a velocity threshold so quick flicks still register.
 *
 * Implemented as a `@Composable` extension on [Modifier] (rather than
 * `Modifier.composed { }`, which is deprecated since Compose UI 1.7) so the
 * required composition locals can be captured at composition time without
 * spinning up an inner composition for every modifier instance.
 */
@Composable
internal fun Modifier.editorContentSwipe(
    enabled: Boolean,
    showPreview: Boolean,
    onSwipeToPreview: () -> Unit,
    onSwipeToEdit: () -> Unit
): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val haptics = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current

    val distanceThresholdPx = with(density) { SWIPE_DISTANCE_THRESHOLD_DP.dp.toPx() }
    val velocityThresholdPxPerSec = with(density) { SWIPE_VELOCITY_THRESHOLD_DP_PER_SEC.dp.toPx() }
    val touchSlop = viewConfig.touchSlop

    return then(
        Modifier.pointerInput(enabled, showPreview, layoutDirection) {
            if (!enabled) return@pointerInput
            detectHorizontalSwipes(
                layoutDirection = layoutDirection,
                touchSlop = touchSlop,
                distanceThresholdPx = distanceThresholdPx,
                velocityThresholdPxPerSec = velocityThresholdPxPerSec,
                showPreview = showPreview,
                haptics = haptics,
                onSwipeToPreview = onSwipeToPreview,
                onSwipeToEdit = onSwipeToEdit
            )
        }
    )
}

/**
 * Coroutine body for the swipe gesture. Extracted from [editorContentSwipe] to
 * keep the composable thin and to make the gesture state machine independently
 * readable. Runs inside the [androidx.compose.ui.input.pointer.PointerInputScope]
 * provided by [pointerInput].
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectHorizontalSwipes(
    layoutDirection: LayoutDirection,
    touchSlop: Float,
    distanceThresholdPx: Float,
    velocityThresholdPxPerSec: Float,
    showPreview: Boolean,
    haptics: HapticFeedback,
    onSwipeToPreview: () -> Unit,
    onSwipeToEdit: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val velocityTracker = VelocityTracker().apply {
            addPosition(down.uptimeMillis, down.position)
        }

        var totalDx = 0f
        var totalDy = 0f
        var horizontalLocked = false
        var triggered = false

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            val delta = change.positionChange()
            totalDx += delta.x
            totalDy += delta.y
            velocityTracker.addPosition(change.uptimeMillis, change.position)

            if (!horizontalLocked && shouldLockHorizontal(totalDx, totalDy, touchSlop)) {
                horizontalLocked = true
            }

            if (horizontalLocked) {
                change.consume()

                if (!triggered) {
                    val signedDx = totalDx.withRtlSign(layoutDirection)
                    val signedVelocityX =
                        velocityTracker.calculateVelocity().x.withRtlSign(layoutDirection)
                    if (shouldTriggerSwipe(
                            signedDx = signedDx,
                            signedVelocityX = signedVelocityX,
                            touchSlop = touchSlop,
                            distanceThresholdPx = distanceThresholdPx,
                            velocityThresholdPxPerSec = velocityThresholdPxPerSec
                        )
                    ) {
                        triggered = dispatchSwipe(
                            signedDx = signedDx,
                            showPreview = showPreview,
                            haptics = haptics,
                            onSwipeToPreview = onSwipeToPreview,
                            onSwipeToEdit = onSwipeToEdit
                        )
                    }
                }
            }
        }
    }
}

/** Locks the gesture to horizontal once the user has clearly committed to a sideways drag. */
private fun shouldLockHorizontal(totalDx: Float, totalDy: Float, touchSlop: Float): Boolean =
    abs(totalDx) > touchSlop &&
        abs(totalDx) > abs(totalDy) * SWIPE_HORIZONTAL_DOMINANCE_RATIO

/** A swipe fires either by reaching the distance threshold or via a fast enough flick. */
private fun shouldTriggerSwipe(
    signedDx: Float,
    signedVelocityX: Float,
    touchSlop: Float,
    distanceThresholdPx: Float,
    velocityThresholdPxPerSec: Float
): Boolean {
    val triggerByDistance = abs(signedDx) >= distanceThresholdPx
    val triggerByVelocity = abs(signedVelocityX) >= velocityThresholdPxPerSec &&
        abs(signedDx) >= touchSlop * 2
    return triggerByDistance || triggerByVelocity
}

/**
 * Dispatches the swipe to the appropriate callback based on direction and current
 * mode. Returns whether a callback actually fired so the caller can latch the
 * gesture and avoid re-triggering during the same drag.
 */
private fun dispatchSwipe(
    signedDx: Float,
    showPreview: Boolean,
    haptics: HapticFeedback,
    onSwipeToPreview: () -> Unit,
    onSwipeToEdit: () -> Unit
): Boolean {
    val toPreview = signedDx < 0
    return when {
        toPreview && !showPreview -> {
            onSwipeToPreview()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            true
        }
        !toPreview && showPreview -> {
            onSwipeToEdit()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            true
        }
        else -> false
    }
}

private fun Float.withRtlSign(direction: LayoutDirection): Float =
    if (direction == LayoutDirection.Rtl) -this else this
