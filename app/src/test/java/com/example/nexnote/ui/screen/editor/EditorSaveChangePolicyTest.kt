package com.example.nexnote.ui.screen.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSaveChangePolicyTest {

    @Test
    fun `hasUnsavedNoteChanges is false when persisted note fields match`() {
        val savedSnapshot = noteState()
        val currentState = savedSnapshot.copy(
            isDirty = true,
            isSaving = true,
            contentVersion = savedSnapshot.contentVersion + 1
        )

        assertFalse(
            EditorSaveChangePolicy.hasUnsavedNoteChanges(savedSnapshot, currentState)
        )
    }

    @Test
    fun `hasUnsavedNoteChanges catches metadata edits during save`() {
        val savedSnapshot = noteState()
        val currentState = savedSnapshot.copy(
            isMarkdown = !savedSnapshot.isMarkdown,
            backgroundColor = 0x00FFAA,
            imagePaths = savedSnapshot.imagePaths + "images/new.jpg"
        )

        assertTrue(
            EditorSaveChangePolicy.hasUnsavedNoteChanges(savedSnapshot, currentState)
        )
    }

    @Test
    fun `hasUnsavedTemplateChanges catches markdown edits during save`() {
        val savedSnapshot = templateState()
        val currentState = savedSnapshot.copy(isMarkdown = !savedSnapshot.isMarkdown)

        assertTrue(
            EditorSaveChangePolicy.hasUnsavedTemplateChanges(savedSnapshot, currentState)
        )
    }

    @Test
    fun `hasUnsavedTemplateChanges ignores equivalent title trimming`() {
        val savedSnapshot = templateState(title = "Template")
        val currentState = savedSnapshot.copy(title = "  Template  ")

        assertFalse(
            EditorSaveChangePolicy.hasUnsavedTemplateChanges(savedSnapshot, currentState)
        )
    }

    private fun noteState(): EditorUiState {
        return EditorUiState(
            noteId = 7L,
            title = "Title",
            content = "Body",
            isMarkdown = true,
            showPreview = false,
            creationDate = 1_000L,
            timezone = "Europe/Rome",
            isPinned = true,
            imagePaths = listOf("images/one.jpg"),
            backgroundColor = 0xFFFFFF,
            isDirty = false,
            contentVersion = 1
        )
    }

    private fun templateState(title: String = "Template"): EditorUiState {
        return EditorUiState(
            templateId = 3L,
            isTemplateMode = true,
            title = title,
            content = "Template body",
            isMarkdown = false,
            isDirty = false,
            contentVersion = 1
        )
    }
}
