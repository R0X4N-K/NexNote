package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteShareTextTest {

    @Test
    fun `shareAsText preserves markdown source for share targets`() {
        val notes = listOf(
            Note(title = "One", content = "**Bold**", isMarkdown = true),
            Note(title = "Two", content = "# Heading", isMarkdown = true)
        )

        assertEquals(
            "One\n\n**Bold**\n\nTwo\n\n# Heading",
            notes.shareAsText()
        )
    }

    @Test
    fun `shareSubject uses single note title when available`() {
        val notes = listOf(Note(title = "Roadmap", content = "Next"))

        assertEquals("Roadmap", notes.shareSubject())
    }

    @Test
    fun `shareSubject summarizes multi note shares`() {
        val notes = listOf(
            Note(title = "One", content = "A"),
            Note(title = "Two", content = "B")
        )

        assertEquals("2 NexNote notes", notes.shareSubject())
    }
}
