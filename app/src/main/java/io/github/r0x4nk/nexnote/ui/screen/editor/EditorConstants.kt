package io.github.r0x4nk.nexnote.ui.screen.editor

internal const val TAG_SCROLL_VISIBILITY_THRESHOLD_PX = 28
internal const val TAG_SCROLL_REVEAL_THRESHOLD_PX = 10
internal const val CONTENT_SCROLL_ANCHOR_FRACTION = 0.5f
internal const val DEFAULT_UNDO_HISTORY_DEBOUNCE_MS = 400L
internal const val DEFAULT_UNDO_HISTORY_MAX_SNAPSHOTS = 50
internal const val CONTENT_MODEL_SYNC_DEBOUNCE_MS = 250L
internal const val MAX_CONTENT_LENGTH = 500_000
// Keeps heavy first preview composition out of the navigation enter animation.
internal const val DIRECT_PREVIEW_FIRST_COMPOSITION_DELAY_MS = 220L
internal const val MARKDOWN_CHECKLIST_SNIPPET = "\n- [ ] "
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
