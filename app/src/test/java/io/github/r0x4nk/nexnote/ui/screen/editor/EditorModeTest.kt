package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorModeTest {

    @Test
    fun `fromRoute returns new note when all route ids are unused`() {
        assertEquals(EditorMode.NewNote, EditorMode.fromRoute())
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
}
