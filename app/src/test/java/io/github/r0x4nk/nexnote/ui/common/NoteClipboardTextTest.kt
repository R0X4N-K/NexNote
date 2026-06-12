package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteClipboardTextTest {

    @Test
    fun `copyAsPlainText includes title and removes markdown syntax`() {
        val note = Note(
            title = "Release notes",
            content = "# Highlights\n**Fast** sync",
            isMarkdown = true
        )

        assertEquals(
            "Release notes\n\nHighlights\nFast sync",
            note.copyAsPlainText()
        )
    }

    @Test
    fun `copyAsMarkdown includes title and preserves markdown body`() {
        val note = Note(
            title = "Release notes",
            content = "# Highlights\n**Fast** sync",
            isMarkdown = true
        )

        assertEquals(
            "Release notes\n\n# Highlights\n**Fast** sync",
            note.copyAsMarkdown()
        )
    }

    @Test
    fun `copyAsPlainText for note collection removes markdown from every note`() {
        val notes = listOf(
            Note(title = "One", content = "**Bold**", isMarkdown = true),
            Note(title = "Two", content = "[Link](https://example.com)", isMarkdown = true)
        )

        assertEquals(
            "One\n\nBold\n\nTwo\n\nLink",
            notes.copyAsPlainText()
        )
    }

    @Test
    fun `copyAsMarkdown for note collection preserves markdown source`() {
        val notes = listOf(
            Note(title = "One", content = "**Bold**", isMarkdown = true),
            Note(title = "Two", content = "[Link](https://example.com)", isMarkdown = true)
        )

        assertEquals(
            "One\n\n**Bold**\n\nTwo\n\n[Link](https://example.com)",
            notes.copyAsMarkdown()
        )
    }
}
