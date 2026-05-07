package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlin.math.abs

internal enum class TagBarVisibilityRequest {
    Show,
    Hide
}

/**
 * Converts raw editor scroll offsets into stable tag-bar visibility requests.
 *
 * A positive scroll delta means the note text is moving upward on screen, so the
 * tag bar should collapse. A negative delta means the note text is moving
 * downward on screen, so the tag bar should appear again.
 */
internal class TagBarScrollVisibilityController(
    initialScroll: Int,
    initialMaxScroll: Int,
    private val thresholdPx: Int = TAG_SCROLL_VISIBILITY_THRESHOLD_PX,
    private val revealThresholdPx: Int = thresholdPx.coerceAtMost(TAG_SCROLL_REVEAL_THRESHOLD_PX)
) {
    private var previousScroll = initialScroll.coerceIn(0, initialMaxScroll.coerceAtLeast(0))
    private var previousMaxScroll = initialMaxScroll.coerceAtLeast(0)
    private var trackedDirection = ScrollDirection.None
    private var accumulatedDistance = 0

    fun onScrollChanged(
        currentScroll: Int,
        currentMaxScroll: Int,
        isScrollInProgress: Boolean
    ): TagBarVisibilityRequest? {
        val normalizedMaxScroll = currentMaxScroll.coerceAtLeast(0)
        val normalizedScroll = currentScroll.coerceIn(0, normalizedMaxScroll)
        val maxScrollChanged = normalizedMaxScroll != previousMaxScroll
        val delta = normalizedScroll - previousScroll

        previousScroll = normalizedScroll
        previousMaxScroll = normalizedMaxScroll

        // Tag-bar animations resize the editor viewport. When the user is near
        // the bottom, Compose clamps the scroll value to the new max and emits a
        // delta that did not come from the gesture, so it must not flip the bar.
        if (maxScrollChanged) {
            resetGesture()
            return null
        }

        if (!isScrollInProgress || delta == 0) {
            resetGesture()
            return null
        }

        val direction = ScrollDirection.fromDelta(delta)
        if (direction != trackedDirection) {
            trackedDirection = direction
            accumulatedDistance = 0
        }

        accumulatedDistance += abs(delta)
        val directionThresholdPx = when (direction) {
            ScrollDirection.TextMovesDown -> revealThresholdPx
            ScrollDirection.TextMovesUp -> thresholdPx
            ScrollDirection.None -> thresholdPx
        }
        if (accumulatedDistance < directionThresholdPx) return null

        accumulatedDistance = 0
        return when (direction) {
            ScrollDirection.TextMovesUp -> TagBarVisibilityRequest.Hide
            ScrollDirection.TextMovesDown -> TagBarVisibilityRequest.Show
            ScrollDirection.None -> null
        }
    }

    fun syncTo(currentScroll: Int, currentMaxScroll: Int) {
        val normalizedMaxScroll = currentMaxScroll.coerceAtLeast(0)
        previousScroll = currentScroll.coerceIn(0, normalizedMaxScroll)
        previousMaxScroll = normalizedMaxScroll
        resetGesture()
    }

    private fun resetGesture() {
        trackedDirection = ScrollDirection.None
        accumulatedDistance = 0
    }
}

private enum class ScrollDirection {
    None,
    TextMovesUp,
    TextMovesDown;

    companion object {
        fun fromDelta(delta: Int): ScrollDirection = when {
            delta > 0 -> TextMovesUp
            delta < 0 -> TextMovesDown
            else -> None
        }
    }
}
