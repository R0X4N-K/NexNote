package io.github.r0x4nk.nexnote.ui.screen.editor

internal const val TAG_SCROLL_VISIBILITY_THRESHOLD_PX = 28
internal const val TAG_SCROLL_REVEAL_THRESHOLD_PX = 10

/**
 * Lockout window applied after a tag-bar visibility flip.
 *
 * The tag row sits inside an [androidx.compose.animation.AnimatedVisibility]
 * whose enter/exit animations (≈190 ms expand, ≈160 ms shrink) resize the
 * editor viewport on every frame. While the viewport is in flux, the
 * underlying scroll trackers can briefly move backwards (the
 * [androidx.compose.foundation.ScrollState.value] clamp in edit mode, or the
 * [androidx.compose.foundation.lazy.LazyListState] reflow when a trailing
 * image at the end of the note settles its aspect ratio in preview mode).
 * Without a cooldown those layout-induced shifts get misread as the user
 * reversing scroll direction, which flips the bar back open and starts an
 * oscillation that prevents the user from reaching the bottom of the note.
 *
 * 240 ms comfortably covers both transitions plus a few settle frames while
 * still feeling responsive when the user genuinely changes direction.
 */
internal const val TAG_BAR_VISIBILITY_COOLDOWN_MS = 240L
internal const val CONTENT_SCROLL_ANCHOR_FRACTION = 0.5f
internal const val DEFAULT_UNDO_HISTORY_DEBOUNCE_MS = 400L
internal const val DEFAULT_UNDO_HISTORY_MAX_SNAPSHOTS = 50
internal const val CONTENT_MODEL_SYNC_DEBOUNCE_MS = 250L
internal const val MAX_CONTENT_LENGTH = 500_000
// Keeps heavy first preview composition out of the navigation enter animation.
internal const val DIRECT_PREVIEW_FIRST_COMPOSITION_DELAY_MS = 220L
// Keeps long edit-mode text field hydration out of the navigation enter animation.
internal const val DIRECT_EDIT_TEXT_FIELD_SYNC_DELAY_MS = DIRECT_PREVIEW_FIRST_COMPOSITION_DELAY_MS
internal const val DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS = 2_000
internal const val MARKDOWN_WEB_LINK_SNIPPET = "\n[text](url)"
internal const val EDITOR_MARKDOWN_ENABLED = true
internal val SWIPE_DISTANCE_THRESHOLD_DP = 56
internal val SWIPE_VELOCITY_THRESHOLD_DP_PER_SEC = 350
internal const val SWIPE_HORIZONTAL_DOMINANCE_RATIO = 1.2f

// Background pre-parse: debounce time before parsing content for preview cache warmth.
internal const val BACKGROUND_PREPARSE_DEBOUNCE_MS = 800L
// Skip background pre-parse for very short notes where parsing is trivially fast.
internal const val PREPARSE_MIN_CHARS = 500

// Ordered by hue. null represents "no custom color" (theme default surface).
internal val NOTE_COLOR_PALETTE: List<Int?> = listOf(
    null,
    0xFFFFCDD2.toInt(), // Soft red
    0xFFFFE0B2.toInt(), // Soft orange
    0xFFFFF9C4.toInt(), // Soft yellow
    0xFFC8E6C9.toInt(), // Soft green
    0xFFBBDEFB.toInt(), // Soft blue
    0xFFE1BEE7.toInt(), // Soft purple
    0xFFD7CCC8.toInt(), // Warm grey
)
