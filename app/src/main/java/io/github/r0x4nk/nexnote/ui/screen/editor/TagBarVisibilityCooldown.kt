package io.github.r0x4nk.nexnote.ui.screen.editor

/**
 * Temporal lockout that prevents the tag-bar visibility from flipping back and
 * forth while its [androidx.compose.animation.AnimatedVisibility] transition
 * is still resizing the editor viewport.
 *
 * The auto-hide trackers infer the user's scroll direction from raw scroll
 * state (pixel offset in edit mode, lazy item index/offset in preview mode).
 * Both backends can briefly reverse direction *because of* a tag-bar
 * animation, not because of a real gesture:
 *  - in edit mode the [androidx.compose.foundation.ScrollState] clamps its
 *    value to the new max once the viewport grows;
 *  - in preview mode the [androidx.compose.foundation.lazy.LazyListState]
 *    reflows its first-visible item — particularly when a trailing image at
 *    the end of the note settles to its real aspect ratio.
 *
 * Without protection, those layout-induced reversals are interpreted as the
 * user dragging the opposite way and the bar oscillates between visible and
 * hidden, blocking the user from reaching the bottom of the note. This class
 * solves the issue by accepting only same-direction requests for a short
 * window after every honored emit. Same-direction repeats are always allowed
 * so the bar still settles to the most recent scroll intent if the user keeps
 * dragging past the cooldown.
 *
 * The clock is injectable to keep the class deterministic in unit tests.
 */
internal class TagBarVisibilityCooldown(
    private val cooldownMs: Long = TAG_BAR_VISIBILITY_COOLDOWN_MS,
    private val nowMs: () -> Long = DefaultClockMs
) {
    private var lastEmitMs: Long = Long.MIN_VALUE
    private var lastEmittedRequest: TagBarVisibilityRequest? = null

    /**
     * Returns [request] when it can be forwarded to the UI, or `null` while
     * the cooldown window is still blocking an opposite-direction flip after
     * a recent emit. Honored requests update the internal state, so the next
     * opposite request restarts the window from the moment of the actual
     * flip.
     */
    fun accept(request: TagBarVisibilityRequest): TagBarVisibilityRequest? {
        val previous = lastEmittedRequest
        if (previous != null && previous != request) {
            val elapsedMs = nowMs() - lastEmitMs
            if (elapsedMs < cooldownMs) return null
        }
        lastEmittedRequest = request
        lastEmitMs = nowMs()
        return request
    }

    /**
     * Clears the cooldown so the next request — in either direction — is
     * forwarded immediately. Call this when the auto-hide pipeline transitions
     * to a state that should not inherit prior gestures (e.g. the user pinned
     * the bar, started a programmatic scroll, or switched between edit and
     * preview modes).
     */
    fun reset() {
        lastEmittedRequest = null
        lastEmitMs = Long.MIN_VALUE
    }
}

/**
 * Default monotonic clock used by [TagBarVisibilityCooldown] in production.
 * Tests inject a deterministic counter instead — see
 * `TagBarVisibilityCooldownTest`.
 */
private val DefaultClockMs: () -> Long = { System.nanoTime() / 1_000_000L }
