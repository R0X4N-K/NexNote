package io.github.r0x4nk.nexnote.ui.component

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTagFoldersTest {

    @Test
    fun `buildNoteTagFolders groups notes by every tag and keeps untagged last`() {
        val work = scoredNote(id = 1L, content = "Ship #work #android")
        val home = scoredNote(id = 2L, content = "Plan #home")
        val untagged = scoredNote(id = 3L, content = "Plain note")

        val folders = buildNoteTagFolders(listOf(work, home, untagged))

        assertEquals(listOf("#android", "#home", "#work", "Untagged"), folders.map { it.title })
        assertEquals(listOf(1L), folders[0].items.map { it.note.id })
        assertEquals(listOf(2L), folders[1].items.map { it.note.id })
        assertEquals(listOf(1L), folders[2].items.map { it.note.id })
        assertEquals(listOf(3L), folders[3].items.map { it.note.id })
    }

    @Test
    fun `buildNoteTagFolders preserves note order inside each folder`() {
        val older = scoredNote(id = 1L, content = "#work older")
        val newer = scoredNote(id = 2L, content = "#work newer")

        val folders = buildNoteTagFolders(listOf(newer, older))

        assertEquals(listOf("#work"), folders.map { it.title })
        assertEquals(listOf(2L, 1L), folders.single().items.map { it.note.id })
    }

    private fun scoredNote(
        id: Long,
        content: String
    ): ScoredNote = ScoredNote(
        note = Note(id = id, content = content),
        score = 0,
        titleRanges = emptyList(),
        contentRanges = emptyList()
    )
}
