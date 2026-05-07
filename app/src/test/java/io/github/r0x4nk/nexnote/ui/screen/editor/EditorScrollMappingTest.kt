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
    fun `preview bottom offset aligns tall last item with viewport bottom`() {
        assertEquals(600, previewBottomScrollOffset(itemHeight = 1000, viewportHeight = 400))
    }

    @Test
    fun `preview bottom offset lets short last item clamp to list end`() {
        assertEquals(0, previewBottomScrollOffset(itemHeight = 240, viewportHeight = 400))
    }
}
