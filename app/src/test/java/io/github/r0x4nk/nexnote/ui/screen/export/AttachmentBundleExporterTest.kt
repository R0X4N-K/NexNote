package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.util.AttachmentMarkdown
import java.util.zip.ZipFile
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AttachmentBundleExporterTest {
    @get:Rule val folder = TemporaryFolder()
    private val path = "images/attachments/note_1_abcd.pdf"
    private fun note() = Note(content = AttachmentMarkdown.format(NoteAttachment(path, "Report final.pdf")), imagePaths = listOf(path))

    @Test fun `bundle preserves bytes and rewrites Markdown to portable paths`() {
        val note = note()
        val document = folder.newFile("note.md").apply { writeText(note.content) }
        val source = folder.newFile("source.pdf").apply { writeBytes(byteArrayOf(0, 1, 2, -1)) }
        val destination = folder.newFile("bundle.zip")
        AttachmentBundleExporter.write(destination, document, "note.md", listOf(note), { source })
        ZipFile(destination).use { zip ->
            assertEquals(2, zip.size())
            val markdown = zip.getInputStream(zip.getEntry("note.md")).bufferedReader().use { it.readText() }
            assertTrue(markdown.contains("attachments/1_Report_final.pdf"))
            assertFalse(markdown.contains(path))
            assertArrayEquals(source.readBytes(), zip.getInputStream(zip.getEntry("attachments/1_Report_final.pdf")).use { it.readBytes() })
        }
    }

    @Test fun `long or extensionless names keep the payload extension in the bundle`() {
        val attachment = NoteAttachment(path, "A".repeat(180))
        val note = note().copy(content = AttachmentMarkdown.format(attachment))
        val document = folder.newFile("note.md").apply { writeText(note.content) }
        val source = folder.newFile("source.pdf")
        val destination = folder.newFile("bundle.zip")
        AttachmentBundleExporter.write(destination, document, "note.md", listOf(note), { source })
        ZipFile(destination).use { zip ->
            val entry = zip.entries().asSequence().first { it.name.startsWith("attachments/") }
            assertTrue(entry.name.endsWith(".pdf"))
            assertTrue(entry.name.substringAfterLast('/').length <= 122)
        }
    }

    @Test fun `undo retained and unowned files are not exported`() {
        assertFalse(AttachmentBundleExporter.needsBundle(listOf(note().copy(content = "Deleted link"))))
        assertFalse(AttachmentBundleExporter.needsBundle(listOf(note().copy(imagePaths = emptyList()))))
        assertTrue(AttachmentBundleExporter.needsBundle(listOf(note())))
        assertTrue(AttachmentBundleExporter.needsBundle(listOf(note().copy(content = "See " + note().content))))
    }

    @Test fun `missing encrypted and cancelled files remove incomplete bundles`() {
        val document = folder.newFile("note.pdf")
        val encrypted = folder.newFile("encrypted").apply { writeText("nexnote-vault-file:1:AES-GCM:iv:cipher") }
        for (source in listOf(encrypted, java.io.File(folder.root, "missing"))) {
            val destination = java.io.File(folder.root, "bundle.zip")
            assertTrue(runCatching {
                AttachmentBundleExporter.write(destination, document, "note.pdf", listOf(note()), { source })
            }.isFailure)
            assertFalse(destination.exists())
        }
        val destination = java.io.File(folder.root, "cancel.zip")
        assertTrue(runCatching {
            AttachmentBundleExporter.write(destination, document, "note.pdf", listOf(note()), { encrypted },
                { throw CancellationException() })
        }.exceptionOrNull() is CancellationException)
        assertFalse(destination.exists())
    }
}
