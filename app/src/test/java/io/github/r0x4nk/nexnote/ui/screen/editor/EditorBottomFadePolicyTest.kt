package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorBottomFadePolicyTest {

    @Test
    fun `bottom fade is visible in both content modes`() {
        assertTrue(editorBottomFadeVisible(EditorUiState(showPreview = false)))
        assertTrue(editorBottomFadeVisible(EditorUiState(showPreview = true)))
    }

    @Test
    fun `bottom fade remains part of the template editor surface`() {
        assertTrue(editorBottomFadeVisible(EditorUiState(isTemplateMode = true)))
    }

    @Test
    fun `bottom fade is hidden while content is unavailable`() {
        assertFalse(editorBottomFadeVisible(EditorUiState(isLoading = true)))
        assertFalse(editorBottomFadeVisible(EditorUiState(isVaultLocked = true)))
    }
}
