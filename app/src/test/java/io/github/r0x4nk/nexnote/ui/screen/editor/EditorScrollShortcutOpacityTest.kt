package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorScrollShortcutOpacityTest {

    @Test
    fun `alpha remains normal when cursor is away from shortcut area`() {
        assertEquals(
            EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA,
            editorScrollShortcutAlpha(
                cursorBoundsInViewportPx = EditorCursorVerticalBounds(
                    topPx = 76f,
                    bottomPx = 100f
                ),
                viewportHeightPx = 1000
            ),
            0.001f
        )
    }

    @Test
    fun `alpha is reduced when cursor reaches shortcut area`() {
        assertEquals(
            EDITOR_SCROLL_SHORTCUT_REDUCED_ALPHA,
            editorScrollShortcutAlpha(
                cursorBoundsInViewportPx = EditorCursorVerticalBounds(
                    topPx = 736f,
                    bottomPx = 760f
                ),
                viewportHeightPx = 1000
            ),
            0.001f
        )
    }

    @Test
    fun `alpha is reduced when cursor is just above shortcut area`() {
        assertEquals(
            EDITOR_SCROLL_SHORTCUT_REDUCED_ALPHA,
            editorScrollShortcutAlpha(
                cursorBoundsInViewportPx = EditorCursorVerticalBounds(
                    topPx = 596f,
                    bottomPx = 620f
                ),
                viewportHeightPx = 1000
            ),
            0.001f
        )
    }

    @Test
    fun `alpha remains normal before cursor metrics are available`() {
        assertEquals(
            EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA,
            editorScrollShortcutAlpha(
                cursorBoundsInViewportPx = null,
                viewportHeightPx = 1000
            ),
            0.001f
        )
    }

    @Test
    fun `cursor metric uses nearest valid offset while text layout is stale`() {
        val cursorBounds = editorCursorBoundsInViewportPx(
            layoutTextLength = 4,
            currentTextLength = 5,
            cursorOffset = 5,
            scrollOffsetPx = 0
        ) { safeOffset ->
            assertEquals(4, safeOffset)
            EditorCursorVerticalBounds(
                topPx = 80f,
                bottomPx = 104f
            )
        }

        assertEquals(80f, cursorBounds?.topPx ?: 0f, 0.001f)
        assertEquals(104f, cursorBounds?.bottomPx ?: 0f, 0.001f)
    }

    @Test
    fun `cursor offset is bounded to the current text layout`() {
        val cursorBounds = editorCursorBoundsInViewportPx(
            layoutTextLength = 4,
            currentTextLength = 4,
            cursorOffset = 12,
            scrollOffsetPx = 24
        ) { safeOffset ->
            assertEquals(4, safeOffset)
            EditorCursorVerticalBounds(
                topPx = 100f,
                bottomPx = 124f
            )
        }

        assertEquals(76f, cursorBounds?.topPx ?: 0f, 0.001f)
        assertEquals(100f, cursorBounds?.bottomPx ?: 0f, 0.001f)
    }
}
