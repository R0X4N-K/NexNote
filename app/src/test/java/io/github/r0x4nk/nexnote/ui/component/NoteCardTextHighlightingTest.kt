package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteCardTextHighlightingTest {

    @Test
    fun `card markdown text renders inline and list syntax`() {
        val text = buildNoteCardDisplayText(
            sourceText = "**Bold** and [Docs](https://example.com)\n- [x] Done",
            ranges = emptyList(),
            linkColor = Color.Blue,
            highlightColor = Color.Red,
            renderMarkdown = true
        )

        assertEquals("Bold and Docs\n\u2611 Done", text.text)
    }

    @Test
    fun `card markdown text renders standalone images as alt text`() {
        val text = buildNoteCardDisplayText(
            sourceText = "![Diagram](images/diagram.png)",
            ranges = emptyList(),
            linkColor = Color.Blue,
            highlightColor = Color.Red,
            renderMarkdown = true
        )

        assertEquals("Diagram", text.text)
    }

    @Test
    fun `card markdown text normalizes heading scale`() {
        val text = buildNoteCardDisplayText(
            sourceText = "# Large heading",
            ranges = emptyList(),
            linkColor = Color.Blue,
            highlightColor = Color.Red,
            renderMarkdown = true
        )

        assertEquals("Large heading", text.text)
        assertTrue(text.spanStyles.none { range -> range.item.fontSize != TextUnit.Unspecified })
    }

    @Test
    fun `card markdown highlights source ranges after rendering`() {
        val text = buildNoteCardDisplayText(
            sourceText = "**Bold** text",
            ranges = listOf(2..5),
            linkColor = Color.Blue,
            highlightColor = Color.Red,
            renderMarkdown = true
        )

        val highlight = text.spanStyles.firstOrNull { range ->
            range.item.background != Color.Unspecified
        }
        assertNotNull(highlight)
        assertEquals(0, highlight?.start)
        assertEquals(4, highlight?.end)
    }
}
