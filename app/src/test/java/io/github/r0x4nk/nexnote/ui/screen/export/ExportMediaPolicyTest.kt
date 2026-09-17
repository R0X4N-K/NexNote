package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.domain.model.Note
import org.junit.Assert.*
import org.junit.Test

class ExportMediaPolicyTest {
    @Test fun excludingMediaRetainsTextAndWebLinksWithoutInternalDestinations() {
        val note = Note(
            content = "Before ![Photo](images/photo.jpg)\n[Report](images/attachments/note_1_abcd.pdf)\n[Website](https://example.com)",
            imagePaths = listOf("images/photo.jpg", "images/attachments/note_1_abcd.pdf")
        )
        val exported = ExportMediaPolicy.apply(listOf(note), false, ExportFormat.TXT).single()
        assertEquals("Before Photo\nReport\n[Website](https://example.com)", exported.content)
        assertTrue(exported.imagePaths.isEmpty())
        assertEquals(2, note.imagePaths.size)
    }

    @Test fun includingMediaPreservesOriginalNotes() {
        val notes = listOf(Note(content = "![Photo](images/photo.jpg)"))
        assertSame(notes, ExportMediaPolicy.apply(notes, true, ExportFormat.TXT))
    }

    @Test fun plainTextNotesKeepLiteralMarkdownSyntax() {
        val note = Note(content = "![example](images/example.jpg)", isMarkdown = false)
        assertEquals(
            note.content,
            ExportMediaPolicy.apply(listOf(note), false, ExportFormat.TXT).single().content
        )
    }

    @Test fun codeSamplesRemainLiteral() {
        val text = "```md\n![Photo](images/photo.jpg)\n```\n`![Photo](images/photo.jpg)`"
        assertEquals(
            text,
            ExportMediaPolicy.apply(listOf(Note(content = text)), false, ExportFormat.TXT).single().content
        )
    }

    @Test fun pdfKeepsImagesEvenWhenTheZipArchiveIsDisabled() {
        val note = Note(
            content = "Before ![Photo](images/photo.jpg)",
            imagePaths = listOf("images/photo.jpg")
        )

        val exported = ExportMediaPolicy.apply(listOf(note), false, ExportFormat.PDF).single()

        assertEquals(note.content, exported.content)
        assertEquals(note.imagePaths, exported.imagePaths)
    }

    @Test fun pdfStillReducesAttachmentsToLabelsWithoutAZip() {
        val note = Note(
            content = "[Report](images/attachments/note_1_abcd.pdf)",
            imagePaths = listOf("images/attachments/note_1_abcd.pdf")
        )

        val exported = ExportMediaPolicy.apply(listOf(note), false, ExportFormat.PDF).single()

        assertEquals("Report", exported.content)
    }
}
