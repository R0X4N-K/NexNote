package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorKeyboardToolbarVisibilityPolicyTest {

    @Test
    fun `shows toolbar while content text is selected with keyboard closed`() {
        assertTrue(
            toolbarVisible(
                isKeyboardVisible = false,
                contentSelection = TextRange(2, 8)
            )
        )
    }

    @Test
    fun `hides toolbar when keyboard is closed and content selection is collapsed`() {
        assertFalse(
            toolbarVisible(
                isKeyboardVisible = false,
                contentSelection = TextRange(4)
            )
        )
    }

    @Test
    fun `keeps toolbar visible while keyboard is open`() {
        assertTrue(
            toolbarVisible(
                isKeyboardVisible = true,
                contentSelection = TextRange(4)
            )
        )
    }

    @Test
    fun `keeps toolbar mounted while a toolbar menu is open`() {
        assertTrue(
            toolbarVisible(
                isKeyboardVisible = false,
                keepOpenForToolbarMenu = true,
                contentSelection = TextRange(4)
            )
        )
    }

    @Test
    fun `does not show selection toolbar outside editable content mode`() {
        val selectedContent = TextRange(2, 8)

        assertFalse(toolbarVisible(contentSelection = selectedContent, showPreview = true))
        assertFalse(toolbarVisible(contentSelection = selectedContent, isReadOnly = true))
        assertFalse(toolbarVisible(contentSelection = selectedContent, isNoteSearchActive = true))
        assertFalse(toolbarVisible(contentSelection = selectedContent, showNoteLinkPicker = true))
        assertFalse(toolbarVisible(contentSelection = selectedContent, isLoading = true))
    }

    private fun toolbarVisible(
        isKeyboardVisible: Boolean = false,
        keepOpenForToolbarMenu: Boolean = false,
        contentSelection: TextRange = TextRange(0),
        showPreview: Boolean = false,
        isReadOnly: Boolean = false,
        isNoteSearchActive: Boolean = false,
        showNoteLinkPicker: Boolean = false,
        isLoading: Boolean = false
    ): Boolean {
        return shouldShowEditorKeyboardToolbar(
            isKeyboardVisible = isKeyboardVisible,
            keepOpenForToolbarMenu = keepOpenForToolbarMenu,
            contentSelection = contentSelection,
            showPreview = showPreview,
            isReadOnly = isReadOnly,
            isNoteSearchActive = isNoteSearchActive,
            showNoteLinkPicker = showNoteLinkPicker,
            isLoading = isLoading
        )
    }
}
