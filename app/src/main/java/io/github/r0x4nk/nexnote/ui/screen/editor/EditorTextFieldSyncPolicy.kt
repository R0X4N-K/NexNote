package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.input.TextFieldValue

internal object EditorTextFieldSyncPolicy {

    /**
     * Model-to-field sync is reserved for real content replacements: initial
     * load, undo/redo, vault locking, or other paths that explicitly advance
     * [EditorUiState.contentVersion].
     *
     * Persistence-only updates, such as assigning the database id after the
     * first autosave of a new note, must not hydrate the editor again. Replaying
     * the model at that point can overwrite still-pending keystrokes and move
     * the cursor back to the beginning.
     */
    fun shouldApplyModelContentSync(
        contentVersion: Int,
        syncedContentVersion: Int
    ): Boolean {
        return contentVersion != syncedContentVersion
    }

    fun modelContentSyncCursor(
        contentVersion: Int,
        selectionOffset: Int?,
        contentLength: Int
    ): Int {
        val cursor = if (contentVersion <= 1) {
            0
        } else {
            selectionOffset ?: contentLength
        }
        return cursor.coerceIn(0, contentLength)
    }

    /**
     * Keeps recomposition from replaying stale text into the state-based editor.
     *
     * Loaded content and editor actions already update both the remembered
     * TextFieldValue and TextFieldState through EditorScreenState.setContentFieldValue.
     * This late recomposition sync is only allowed to reconcile selection for
     * matching text; replacing text here can erase freshly loaded content with an
     * older empty construction value.
     */
    fun canApplyRecomposedValue(
        currentValue: TextFieldValue,
        recomposedValue: TextFieldValue
    ): Boolean {
        return currentValue.text == recomposedValue.text
    }

    /**
     * Defers the first edit-mode hydration for long existing notes so the
     * navigation enter animation can finish before BasicTextField measures the
     * full document.
     */
    fun shouldDeferInitialEditContentSync(
        noteId: Long,
        isLoading: Boolean,
        isTemplateMode: Boolean,
        showPreview: Boolean,
        openedDirectlyInEdit: Boolean,
        contentVersion: Int,
        syncedContentVersion: Int,
        contentLength: Int
    ): Boolean {
        return noteId != EditorViewModel.NO_ID &&
            !isLoading &&
            !isTemplateMode &&
            !showPreview &&
            openedDirectlyInEdit &&
            contentVersion == 1 &&
            syncedContentVersion < contentVersion &&
            contentLength >= DIRECT_EDIT_TEXT_FIELD_SYNC_DEFER_MIN_CHARS
    }
}

internal fun EditorUiState.shouldDeferInitialEditContentSync(
    syncedContentVersion: Int
): Boolean {
    return EditorTextFieldSyncPolicy.shouldDeferInitialEditContentSync(
        noteId = noteId,
        isLoading = isLoading,
        isTemplateMode = isTemplateMode,
        showPreview = showPreview,
        openedDirectlyInEdit = openedDirectlyInEdit,
        contentVersion = contentVersion,
        syncedContentVersion = syncedContentVersion,
        contentLength = content.length
    )
}
