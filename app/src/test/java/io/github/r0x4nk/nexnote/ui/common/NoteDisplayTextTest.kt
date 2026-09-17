package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteDisplayTextTest {

    private val untitled = "Untitled note"

    @Test
    fun `large note label retains only the first meaningful line prefix`() {
        val content = "\n\r\n  First   line " + "x".repeat(2_000_000) + "\nOther"
        assertEquals(
            "First line " + "x".repeat(69),
            Note(content = content).displayLabel(untitledLabel = untitled)
        )
        assertEquals("", Note(content = content).displayLabel(0, untitledLabel = untitled))
        assertEquals(untitled, Note(content = "\n\t").displayLabel(untitledLabel = untitled))
        assertEquals("One two", Note(title = " One\ntwo ").displayLabel(untitledLabel = untitled))
    }

    @Test
    fun `displayLabel prefers the note title`() {
        val note = Note(title = "  Sprint   notes  ", content = "Fallback")

        assertEquals("Sprint notes", note.displayLabel(untitledLabel = untitled))
    }

    @Test
    fun `displayLabel falls back to the first content line`() {
        val note = Note(content = "\n\n  First   useful line  \nSecond line")

        assertEquals("First useful line", note.displayLabel(untitledLabel = untitled))
    }

    @Test
    fun `toTrashedNoteEvent keeps the first label and all note ids`() {
        val event = listOf(
            Note(id = 7L, title = "Private 1"),
            Note(id = 8L, title = "Private 2")
        ).toTrashedNoteEvent(untitledLabel = untitled)

        assertEquals("Private 1", event?.noteLabel)
        assertEquals(listOf(7L, 8L), event?.noteIds)
    }
}
