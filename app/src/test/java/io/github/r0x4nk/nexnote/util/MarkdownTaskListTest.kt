package io.github.r0x4nk.nexnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
