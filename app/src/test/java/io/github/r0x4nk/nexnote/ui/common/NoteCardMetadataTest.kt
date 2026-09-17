package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteCardMetadataTest {

    @Test
    fun `tags are normalised sorted and limited`() {
        val note = Note(content = "#Zeta #alpha #alpha #beta #gamma")

        val metadata = buildNoteCardMetadata(note, maxTags = 3)

        assertEquals(listOf("alpha", "beta", "gamma"), metadata.tags)
    }

    @Test
    fun `notes without hashtags skip tag extraction`() {
        val metadata = buildNoteCardMetadata(Note(content = "No tags here"))

        assertTrue(metadata.tags.isEmpty())
    }

    @Test
    fun `attachment display names come from the markdown link label`() {
        val path = "images/attachments/note_1_abcd.pdf"
        val note = Note(
            content = "Intro\n[Release notes]($path)\n#ideas",
            imagePaths = listOf(path)
        )

        val metadata = buildNoteCardMetadata(note)

        assertEquals(listOf("Release notes"), metadata.attachmentNames)
        assertEquals(1, metadata.attachmentCount)
        assertEquals(0, metadata.imageCount)
    }

    @Test
    fun `attachment falls back to the stored file name when content has no link`() {
        val path = "images/attachments/note_2_dead.pdf"
        val note = Note(content = "Body without link", imagePaths = listOf(path))

        val metadata = buildNoteCardMetadata(note)

        assertEquals(listOf("note_2_dead.pdf"), metadata.attachmentNames)
        assertEquals(1, metadata.attachmentCount)
    }

    @Test
    fun `attachment count exceeds the resolved names when there are many files`() {
        val paths = (1..3).map { index -> "images/attachments/note_$index" + "_abcd.pdf" }
        val note = Note(
            content = paths.mapIndexed { index, path -> "[File $index]($path)" }
                .joinToString(separator = "\n"),
            imagePaths = paths
        )

        val metadata = buildNoteCardMetadata(note, maxAttachments = 2)

        assertEquals(2, metadata.attachmentNames.size)
        assertEquals(3, metadata.attachmentCount)
    }

    @Test
    fun `image count excludes attachments`() {
        val attachment = "images/attachments/note_1_abcd.png"
        val note = Note(
            content = "Body",
            imagePaths = listOf(attachment, "images/photo.png", "images/other.jpg")
        )

        val metadata = buildNoteCardMetadata(note)

        assertEquals(1, metadata.attachmentCount)
        assertEquals(2, metadata.imageCount)
        assertTrue(metadata.hasAttachments)
        assertTrue(metadata.hasImages)
    }
}
