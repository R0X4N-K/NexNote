package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.ui.component.MarkdownSourceRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorScrollMappingTest {

    @Test
    fun `source offset maps to containing preview block`() {
        val ranges = listOf(
            MarkdownSourceRange(start = 0, end = 12),
            MarkdownSourceRange(start = 20, end = 40),
            MarkdownSourceRange(start = 50, end = 80)
        )

        assertEquals(0, ranges.blockIndexForSourceOffset(5))
        assertEquals(1, ranges.blockIndexForSourceOffset(30))
        assertEquals(2, ranges.blockIndexForSourceOffset(79))
    }

    @Test
    fun `source offset between ranges maps to nearest preceding block`() {
        val ranges = listOf(
            MarkdownSourceRange(start = 0, end = 12),
            MarkdownSourceRange(start = 20, end = 40)
        )

        assertEquals(0, ranges.blockIndexForSourceOffset(16))
        assertEquals(1, ranges.blockIndexForSourceOffset(45))
    }

    @Test
    fun `preview scroll offset preserves position inside a block`() {
        val ranges = listOf(MarkdownSourceRange(start = 0, end = 100))

        val startOffset = previewItemScrollOffsetForSourceOffset(
            sourceRanges = ranges,
            itemIndex = 0,
            sourceOffset = 10,
            itemHeight = 1000,
            viewportHeight = 400,
            viewportFraction = 0.35f
        )
        val laterOffset = previewItemScrollOffsetForSourceOffset(
            sourceRanges = ranges,
            itemIndex = 0,
            sourceOffset = 90,
            itemHeight = 1000,
            viewportHeight = 400,
            viewportFraction = 0.35f
        )

        assertEquals(-40, startOffset)
        assertEquals(760, laterOffset)
        assertTrue(laterOffset > startOffset)
    }

    @Test
    fun `preview end anchor is the final lazy item`() {
        assertEquals(4, previewEndAnchorIndex(totalItems = 5))
    }

    @Test
    fun `preview reading progress starts at zero before scrolling`() {
        val progress = previewReadingProgress(
            contentLength = 1_000,
            sourceOffset = 120,
            canScrollBackward = false,
            canScrollForward = true
        )

        assertEquals(0f, progress, 0.0001f)
    }

    @Test
    fun `preview reading progress moves within visible item`() {
        val progress = previewReadingProgress(
            contentLength = 1_000,
            sourceOffset = 375,
            canScrollBackward = true,
            canScrollForward = true
        )

        assertEquals(0.375f, progress, 0.0001f)
    }

    @Test
    fun `preview reading progress reaches one at list end`() {
        val progress = previewReadingProgress(
            contentLength = 1_000,
            sourceOffset = 850,
            canScrollBackward = true,
            canScrollForward = false
        )

        assertEquals(1f, progress, 0.0001f)
    }

    @Test
    fun `preview reading progress is complete when document does not scroll`() {
        val progress = previewReadingProgress(
            contentLength = 1_000,
            sourceOffset = 0,
            canScrollBackward = false,
            canScrollForward = false
        )

        assertEquals(1f, progress, 0.0001f)
    }

    @Test
    fun `preview reading progress is hidden when document does not scroll`() {
        assertTrue(!previewReadingProgressVisible(canScrollBackward = false, canScrollForward = false))
        assertTrue(previewReadingProgressVisible(canScrollBackward = false, canScrollForward = true))
        assertTrue(previewReadingProgressVisible(canScrollBackward = true, canScrollForward = false))
    }

    @Test
    fun `edit scroll progress tracks the text field pixel offset`() {
        val progress = editScrollProgress(
            scrollOffset = 250,
            maxScrollOffset = 1_000
        )

        assertEquals(0.25f, progress, 0.0001f)
    }

    @Test
    fun `edit scroll progress clamps out of range offsets`() {
        assertEquals(
            0f,
            editScrollProgress(scrollOffset = -20, maxScrollOffset = 1_000),
            0.0001f
        )
        assertEquals(
            1f,
            editScrollProgress(scrollOffset = 1_200, maxScrollOffset = 1_000),
            0.0001f
        )
    }

    @Test
    fun `edit scroll progress is hidden when text does not scroll`() {
        assertEquals(1f, editScrollProgress(scrollOffset = 0, maxScrollOffset = 0), 0.0001f)
        assertTrue(!editScrollProgressVisible(maxScrollOffset = 0))
        assertTrue(editScrollProgressVisible(maxScrollOffset = 1))
    }
}
