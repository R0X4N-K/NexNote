package io.github.r0x4nk.nexnote.ui.screen.editor

internal const val EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA = 1f
internal const val EDITOR_SCROLL_SHORTCUT_REDUCED_ALPHA = 0.12f

private const val SHORTCUT_PROXIMITY_VIEWPORT_FRACTION = 0.32f
private const val SHORTCUT_PROXIMITY_MARGIN_VIEWPORT_FRACTION = 0.08f
private const val SHORTCUT_PROXIMITY_MIN_PX = 180f
private const val SHORTCUT_PROXIMITY_MARGIN_MIN_PX = 72f

internal data class EditorCursorVerticalBounds(
    val topPx: Float,
    val bottomPx: Float
)

internal fun editorScrollShortcutAlpha(
    cursorBoundsInViewportPx: EditorCursorVerticalBounds?,
    viewportHeightPx: Int
): Float {
    if (cursorBoundsInViewportPx == null || viewportHeightPx <= 0) {
        return EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA
    }

    val proximityHeight = maxOf(
        SHORTCUT_PROXIMITY_MIN_PX,
        viewportHeightPx * SHORTCUT_PROXIMITY_VIEWPORT_FRACTION
    )
    val proximityMargin = maxOf(
        SHORTCUT_PROXIMITY_MARGIN_MIN_PX,
        viewportHeightPx * SHORTCUT_PROXIMITY_MARGIN_VIEWPORT_FRACTION
    )
    val proximityStart = viewportHeightPx - proximityHeight - proximityMargin
    val proximityEnd = viewportHeightPx + proximityMargin
    val cursorNearShortcutBand =
        cursorBoundsInViewportPx.bottomPx >= proximityStart &&
            cursorBoundsInViewportPx.topPx <= proximityEnd

    return if (cursorNearShortcutBand) {
        EDITOR_SCROLL_SHORTCUT_REDUCED_ALPHA
    } else {
        EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA
    }
}

internal fun editorCursorBoundsInViewportPx(
    layoutTextLength: Int,
    currentTextLength: Int,
    cursorOffset: Int,
    scrollOffsetPx: Int,
    cursorBoundsProvider: (Int) -> EditorCursorVerticalBounds
): EditorCursorVerticalBounds? {
    // TextFieldState usually updates one frame before TextLayoutResult while
    // typing. Query the nearest still-valid offset so the shortcuts fade during
    // that frame instead of jumping back to full opacity over the typed word.
    val safeTextLength = minOf(layoutTextLength, currentTextLength).coerceAtLeast(0)
    val safeCursorOffset = cursorOffset.coerceIn(0, safeTextLength)
    return runCatching {
        val bounds = cursorBoundsProvider(safeCursorOffset)
        EditorCursorVerticalBounds(
            topPx = bounds.topPx - scrollOffsetPx,
            bottomPx = bounds.bottomPx - scrollOffsetPx
        )
    }.getOrNull()
}
