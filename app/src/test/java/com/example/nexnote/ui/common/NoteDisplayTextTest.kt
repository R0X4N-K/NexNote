package com.example.nexnote.ui.common

import com.example.nexnote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteDisplayTextTest {

    @Test
    fun `displayLabel prefers the note title`() {
        val note = Note(title = "  Sprint   notes  ", content = "Fallback")

        assertEquals("Sprint notes", note.displayLabel())
    }

    @Test
    fun `displayLabel falls back to the first content line`() {
        val note = Note(content = "\n\n  First   useful line  \nSecond line")

        assertEquals("First useful line", note.displayLabel())
    }

    @Test
    fun `snackbarMessage includes the note label`() {
        val event = TrashedNoteEvent(noteId = 7L, noteLabel = "Release checklist")

        assertEquals("Moved \"Release checklist\" to trash", event.snackbarMessage())
    }
}
