package com.example.nexnote.ui.screen.editor

internal const val TAG_SCROLL_VISIBILITY_THRESHOLD_PX = 28
internal const val CONTENT_SCROLL_ANCHOR_FRACTION = 0.5f
internal const val DEFAULT_UNDO_HISTORY_DEBOUNCE_MS = 400L
internal const val DEFAULT_UNDO_HISTORY_MAX_SNAPSHOTS = 50
// Keeps heavy first preview composition out of the navigation enter animation.
internal const val DIRECT_PREVIEW_FIRST_COMPOSITION_DELAY_MS = 220L
internal const val MARKDOWN_CHECKLIST_SNIPPET = "\n- [ ] "
internal const val MARKDOWN_WEB_LINK_SNIPPET = "\n[text](url)"

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
