package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.DateUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportNoteFormatterTest {

    @Test
    fun `plain text export renders markdown body`() {
        val note = Note(
            title = "Project",
            content = """
                # Heading
                **Bold** and [link](https://example.com)
                ![cover](images/note_1_img_1.jpg)
            """.trimIndent(),
            creationDate = TEST_DATE
        )

        val result = ExportNoteFormatter.toPlainText(listOf(note))

        assertTrue(result.contains("Project"))
        assertTrue(result.contains(DateUtils.formatDateTime(TEST_DATE)))
        assertTrue(result.contains("Heading"))
        assertTrue(result.contains("Bold and link"))
        assertTrue(result.contains("cover"))
        assertFalse(result.contains("# Heading"))
        assertFalse(result.contains("**Bold**"))
        assertFalse(result.contains("![cover]"))
    }

    @Test
    fun `plain text export keeps non markdown notes literal`() {
        val note = Note(
            content = "**literal markers**",
            isMarkdown = false,
            creationDate = TEST_DATE
        )

        val result = ExportNoteFormatter.toPlainText(listOf(note))

        assertTrue(result.contains("**literal markers**"))
    }

    @Test
    fun `markdown export keeps markdown source`() {
        val note = Note(
            title = "Project",
            content = "![cover](images/note_1_img_1.jpg)\n\n**Bold**",
            creationDate = TEST_DATE
        )

        val result = ExportNoteFormatter.toMarkdown(listOf(note))

        assertTrue(result.contains("# Project"))
        assertTrue(result.contains("![cover](images/note_1_img_1.jpg)"))
        assertTrue(result.contains("**Bold**"))
    }

    @Test
    fun `multiple notes are separated consistently`() {
        val notes = listOf(
            Note(title = "One", content = "First", creationDate = TEST_DATE),
            Note(title = "Two", content = "Second", creationDate = TEST_DATE)
        )

        val result = ExportNoteFormatter.toPlainText(notes)

        assertTrue(result.contains("First\n\n---\n\nTwo"))
    }

    private companion object {
        const val TEST_DATE = 1_700_000_000_000L
    }
}
