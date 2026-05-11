package io.github.r0x4nk.nexnote.ui.screen.editor

private const val MIN_VIEWPORT_HEIGHT_PX = 1

/**
 * Returns the part of an editor viewport that is not covered by bottom chrome.
 *
 * The IME already resizes the editor through window insets. Overlay controls,
 * such as the keyboard toolbar, must be subtracted separately because they sit
 * above the text field without participating in its parent layout.
 */
internal fun unobscuredViewportHeightPx(
    viewportHeightPx: Int,
    bottomObstructionHeightPx: Int
): Int {
    val safeViewportHeight = viewportHeightPx.coerceAtLeast(0)
    val safeBottomObstruction = bottomObstructionHeightPx.coerceAtLeast(0)
    return (safeViewportHeight - safeBottomObstruction)
        .coerceAtLeast(MIN_VIEWPORT_HEIGHT_PX)
}
