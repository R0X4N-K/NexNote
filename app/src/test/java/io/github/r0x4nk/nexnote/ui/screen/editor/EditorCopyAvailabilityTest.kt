package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorCopyAvailabilityTest {

    @Test
    fun `copy is available while editing visible note text`() {
        assertTrue(
            editorCanCopyVisibleNoteText(
                EditorUiState(showPreview = false)
            )
        )
    }

    @Test
    fun `copy is available while previewing visible note text`() {
        assertTrue(
            editorCanCopyVisibleNoteText(
                EditorUiState(showPreview = true)
            )
        )
    }

    @Test
    fun `copy is unavailable while note is loading`() {
        assertFalse(
            editorCanCopyVisibleNoteText(
                EditorUiState(isLoading = true)
            )
        )
    }

    @Test
    fun `copy is unavailable while Vault note is locked`() {
        assertFalse(
            editorCanCopyVisibleNoteText(
                EditorUiState(isVaultLocked = true)
            )
        )
    }
}
