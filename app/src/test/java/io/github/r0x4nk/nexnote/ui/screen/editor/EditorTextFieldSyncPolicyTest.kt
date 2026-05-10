package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextFieldSyncPolicyTest {

    @Test
    fun `canApplyRecomposedValue skips stale empty text over loaded state`() {
        val currentValue = TextFieldValue("Loaded note", TextRange(0))
        val staleValue = TextFieldValue("")

        assertFalse(
            EditorTextFieldSyncPolicy.canApplyRecomposedValue(currentValue, staleValue)
        )
    }

    @Test
    fun `canApplyRecomposedValue skips stale loaded text over cleared state`() {
        val currentValue = TextFieldValue("")
        val staleValue = TextFieldValue("Loaded note", TextRange(11))

        assertFalse(
            EditorTextFieldSyncPolicy.canApplyRecomposedValue(currentValue, staleValue)
        )
    }

    @Test
    fun `canApplyRecomposedValue accepts selection sync for matching text`() {
        val currentValue = TextFieldValue("Loaded note", TextRange(0))
        val recomposedValue = TextFieldValue("Loaded note", TextRange(11))

        assertTrue(
            EditorTextFieldSyncPolicy.canApplyRecomposedValue(currentValue, recomposedValue)
        )
    }

    @Test
    fun `shouldDeferInitialEditContentSync defers long unsynced existing edit note`() {
        assertTrue(
            EditorTextFieldSyncPolicy.shouldDeferInitialEditContentSync(
                noteId = 42L,
                isLoading = false,
                isTemplateMode = false,
                showPreview = false,
                openedDirectlyInEdit = true,
                contentVersion = 1,
                syncedContentVersion = 0,
                contentLength = DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS
            )
        )
    }

    @Test
    fun `shouldDeferInitialEditContentSync does not defer preview note`() {
        assertFalse(
            EditorTextFieldSyncPolicy.shouldDeferInitialEditContentSync(
                noteId = 42L,
                isLoading = false,
                isTemplateMode = false,
                showPreview = true,
                openedDirectlyInEdit = true,
                contentVersion = 1,
                syncedContentVersion = 0,
                contentLength = DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS
            )
        )
    }

    @Test
    fun `shouldDeferInitialEditContentSync does not defer already synced content`() {
        assertFalse(
            EditorTextFieldSyncPolicy.shouldDeferInitialEditContentSync(
                noteId = 42L,
                isLoading = false,
                isTemplateMode = false,
                showPreview = false,
                openedDirectlyInEdit = true,
                contentVersion = 1,
                syncedContentVersion = 1,
                contentLength = DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS
            )
        )
    }

    @Test
    fun `shouldDeferInitialEditContentSync does not defer short content`() {
        assertFalse(
            EditorTextFieldSyncPolicy.shouldDeferInitialEditContentSync(
                noteId = 42L,
                isLoading = false,
                isTemplateMode = false,
                showPreview = false,
                openedDirectlyInEdit = true,
                contentVersion = 1,
                syncedContentVersion = 0,
                contentLength = DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS - 1
            )
        )
    }

    @Test
    fun `shouldDeferInitialEditContentSync does not defer mode switches back to edit`() {
        assertFalse(
            EditorTextFieldSyncPolicy.shouldDeferInitialEditContentSync(
                noteId = 42L,
                isLoading = false,
                isTemplateMode = false,
                showPreview = false,
                openedDirectlyInEdit = false,
                contentVersion = 1,
                syncedContentVersion = 0,
                contentLength = DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS
            )
        )
    }
}
