package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormattingTest {

    @Test
    fun `bold wraps selection`() {
        val result = MarkdownInlineToggle.bold("hello world", TextRange(0, 5))

        assertEquals("**hello** world", result.text)
        assertEquals(TextRange(2, 7), result.selection)
    }

    @Test
    fun `bold unwraps selection with adjacent markers`() {
        val result = MarkdownInlineToggle.bold("**hello** world", TextRange(2, 7))

        assertEquals("hello world", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun `bold inserts placeholder around empty selection`() {
        val result = MarkdownInlineToggle.bold("xy", TextRange(1))

        // A selected placeholder lets the user immediately see the formatting
        // in the preview and overtype it with their own text.
        assertEquals("x**text**y", result.text)
        assertEquals(TextRange(3, 7), result.selection)
    }

    @Test
    fun `italic inserts placeholder around empty selection`() {
        val result = MarkdownInlineToggle.italic("", TextRange(0))

        assertEquals("*text*", result.text)
        assertEquals(TextRange(1, 5), result.selection)
    }

    @Test
    fun `unordered list inserts marker on empty line`() {
        val result = MarkdownLineToggle.unorderedList("", TextRange(0))

        assertEquals("- ", result.text)
    }

    @Test
    fun `ordered list inserts marker on empty line`() {
        val result = MarkdownLineToggle.orderedList("", TextRange(0))

        assertEquals("1. ", result.text)
    }

    @Test
    fun `setHeading promotes plain line to chosen level`() {
        val result = MarkdownLineToggle.setHeading("Title", TextRange(0, 5), level = 2)

        assertEquals("## Title", result.text)
    }

    @Test
    fun `setHeading replaces existing heading level`() {
        val result = MarkdownLineToggle.setHeading("# Title", TextRange(0, 7), level = 3)

        assertEquals("### Title", result.text)
    }

    @Test
    fun `setHeading toggles off when level matches`() {
        val result = MarkdownLineToggle.setHeading("## Title", TextRange(0, 8), level = 2)

        assertEquals("Title", result.text)
    }

    @Test
    fun `link wraps selection and selects url placeholder`() {
        val result = MarkdownInlineToggle.link("open site", TextRange(5, 9))

        assertEquals("open [site](url)", result.text)
        assertEquals(TextRange("open [site](".length, "open [site](url".length), result.selection)
    }

    @Test
    fun `quote adds prefix to every selected line`() {
        val result = MarkdownLineToggle.quote("a\nb\nc", TextRange(0, 3))

        assertEquals("> a\n> b\nc", result.text)
    }

    @Test
    fun `unordered list removes existing prefixes`() {
        val result = MarkdownLineToggle.unorderedList("- a\n- b", TextRange(0, 7))

        assertEquals("a\nb", result.text)
    }

    @Test
    fun `unordered list replaces ordered marker at line start`() {
        val result = MarkdownLineToggle.unorderedList("1. task", TextRange(4))

        assertEquals("- task", result.text)
    }

    @Test
    fun `ordered list replaces checklist marker at line start`() {
        val result = MarkdownLineToggle.orderedList("- [x] done", TextRange(8))

        assertEquals("1. done", result.text)
    }

    @Test
    fun `task list inserts marker at current line start`() {
        val result = MarkdownLineToggle.taskList("write docs", TextRange(5))

        assertEquals("- [ ] write docs", result.text)
    }

    @Test
    fun `task list replaces unordered marker at line start`() {
        val result = MarkdownLineToggle.taskList("- write docs", TextRange(4))

        assertEquals("- [ ] write docs", result.text)
    }
}
