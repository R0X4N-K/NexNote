package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorModeTest {

    @Test
    fun `fromRoute returns new note when all route ids are unused`() {
        assertEquals(EditorMode.NewNote(), EditorMode.fromRoute())
    }

    @Test
    fun `fromRoute carries creation date for new note`() {
        assertEquals(
            EditorMode.NewNote(initialCreationDate = 123_456L),
            EditorMode.fromRoute(creationDate = 123_456L)
        )
    }

    @Test
    fun `fromRoute gives template editing precedence over note and template ids`() {
        assertEquals(
            EditorMode.EditTemplate(9L),
            EditorMode.fromRoute(noteId = 4L, templateId = 7L, editTemplateId = 9L)
        )
    }

    @Test
    fun `fromRoute maps new template sentinel to template creation mode`() {
        assertEquals(
            EditorMode.NewTemplate,
            EditorMode.fromRoute(editTemplateId = EditorMode.NEW_TEMPLATE_ID)
        )
    }

    @Test
    fun `fromRoute gives note precedence over template application`() {
        assertEquals(
            EditorMode.ExistingNote(4L),
            EditorMode.fromRoute(noteId = 4L, templateId = 7L)
        )
    }

    @Test
    fun `fromRoute maps template id to new note from template mode`() {
        assertEquals(
            EditorMode.NewFromTemplate(7L),
            EditorMode.fromRoute(templateId = 7L)
        )
    }

    @Test
    fun `fromRoute maps vault note id to vault note mode`() {
        assertEquals(
            EditorMode.VaultNote(11L),
            EditorMode.fromRoute(noteId = 4L, templateId = 7L, vaultNoteId = 11L)
        )
    }

    @Test
    fun `fromRoute maps vault note creation sentinel to new vault note mode`() {
        assertEquals(
            EditorMode.NewVaultNote,
            EditorMode.fromRoute(
                noteId = 4L,
                vaultNoteId = EditorMode.NEW_VAULT_NOTE_ID
            )
        )
    }

    @Test
    fun `fromRoute maps vault note creation sentinel with template id to vault template mode`() {
        assertEquals(
            EditorMode.NewVaultFromTemplate(7L),
            EditorMode.fromRoute(
                noteId = 4L,
                templateId = 7L,
                vaultNoteId = EditorMode.NEW_VAULT_NOTE_ID
            )
        )
    }
}
