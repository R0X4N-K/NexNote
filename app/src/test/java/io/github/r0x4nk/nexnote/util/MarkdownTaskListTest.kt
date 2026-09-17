package io.github.r0x4nk.nexnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTaskListTest {

    @Test
    fun `resolves nested task marker from its block line`() {
        val markdown = "Intro\n  - [ ] nested task\nOutro"

        val offset = findMarkdownTaskListMarkerOffset(
            markdown = markdown,
            sourceStart = 0,
            sourceEnd = markdown.length,
            lineIndex = 1
        )

        assertEquals(markdown.indexOf("[ ]") + 1, offset)
    }

    @Test
    fun `resolves task marker inside blockquote source`() {
        val markdown = "> - [x] completed\n> - [ ] pending"

        val offset = findMarkdownTaskListMarkerOffset(
            markdown = markdown,
            sourceStart = 0,
            sourceEnd = markdown.length,
            lineIndex = 1
        )

        assertEquals(markdown.lastIndexOf("[ ]") + 1, offset)
    }

    @Test
    fun `toggles unchecked and checked task markers`() {
        val unchecked = "- [ ] task"
        val markerOffset = unchecked.indexOf("[ ]") + 1

        val checked = toggleMarkdownTaskListItem(unchecked, markerOffset)
        val reopened = checked?.let { toggleMarkdownTaskListItem(it, markerOffset) }

        assertEquals("- [x] task", checked)
        assertEquals(unchecked, reopened)
    }

    @Test
    fun `rejects offsets that do not point to a task marker`() {
        assertNull(toggleMarkdownTaskListItem("plain text", markerOffset = 2))
    }

    @Test
    fun `checks every marker preserving indentation and blockquotes`() {
        val markdown = "- [ ] one\n  - [X] two\n> - [ ] three\nplain"

        assertEquals(
            "- [x] one\n  - [x] two\n> - [x] three\nplain",
            setAllMarkdownTaskListItems(markdown, checked = true)
        )
    }

    @Test
    fun `unchecks every marker`() {
        val markdown = "- [x] one\n  * [X] two\n+ [ ] three"

        assertEquals(
            "- [ ] one\n  * [ ] two\n+ [ ] three",
            setAllMarkdownTaskListItems(markdown, checked = false)
        )
    }

    @Test
    fun `presence helpers distinguish checked and unchecked items`() {
        val markdown = "- [ ] open\n- [x] done"

        assertTrue(hasUncheckedMarkdownTaskListItems(markdown))
        assertTrue(hasCheckedMarkdownTaskListItems(markdown))
        assertFalse(hasCheckedMarkdownTaskListItems("- [ ] open"))
        assertFalse(hasUncheckedMarkdownTaskListItems("- [x] done"))
        assertFalse(hasUncheckedMarkdownTaskListItems("no tasks"))
    }

    @Test
    fun `bulk operations ignore fenced code blocks`() {
        val markdown = "- [x] real\n```\n- [ ] sample\n```"

        assertEquals(
            "- [ ] real\n```\n- [ ] sample\n```",
            setAllMarkdownTaskListItems(markdown, checked = false)
        )
        // The only unchecked item lives inside the fence, so the UI gate stays closed.
        assertFalse(hasUncheckedMarkdownTaskListItems(markdown))
        assertTrue(hasCheckedMarkdownTaskListItems(markdown))
    }
}
