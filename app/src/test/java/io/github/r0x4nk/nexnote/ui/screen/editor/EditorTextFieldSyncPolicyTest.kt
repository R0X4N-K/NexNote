package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextFieldSyncPolicyTest {

    @Test
    fun `shouldApplyModelContentSync applies initial unsynced model version`() {
        assertTrue(
            EditorTextFieldSyncPolicy.shouldApplyModelContentSync(
                contentVersion = 0,
                syncedContentVersion = -1
            )
        )
    }

    @Test
    fun `shouldApplyModelContentSync skips persistence only updates`() {
        assertFalse(
            EditorTextFieldSyncPolicy.shouldApplyModelContentSync(
                contentVersion = 0,
                syncedContentVersion = 0
            )
        )
    }

    @Test
    fun `modelContentSyncCursor starts initial content at beginning`() {
        assertEquals(
            0,
            EditorTextFieldSyncPolicy.modelContentSyncCursor(
                contentVersion = 1,
                selectionOffset = 9,
                contentLength = 12
            )
        )
    }

    @Test
    fun `modelContentSyncCursor restores explicit selection after content replacement`() {
        assertEquals(
            7,
            EditorTextFieldSyncPolicy.modelContentSyncCursor(
                contentVersion = 2,
                selectionOffset = 7,
                contentLength = 12
            )
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
