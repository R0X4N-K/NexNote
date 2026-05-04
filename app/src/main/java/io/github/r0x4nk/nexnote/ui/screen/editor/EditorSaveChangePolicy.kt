package io.github.r0x4nk.nexnote.ui.screen.editor

internal object EditorSaveChangePolicy {

    /**
     * Detects edits that happened while a note save was in flight.
     *
     * The save writes one immutable EditorUiState snapshot. If the user changes
     * any persisted field before the repository returns, the current state must
     * stay dirty so the next autosave can persist that newer value.
     */
    fun hasUnsavedNoteChanges(
        savedSnapshot: EditorUiState,
        currentState: EditorUiState
    ): Boolean {
        return savedSnapshot.title != currentState.title ||
            savedSnapshot.content != currentState.content ||
            savedSnapshot.isMarkdown != currentState.isMarkdown ||
            savedSnapshot.showPreview != currentState.showPreview ||
            savedSnapshot.creationDate != currentState.creationDate ||
            savedSnapshot.timezone != currentState.timezone ||
            savedSnapshot.isPinned != currentState.isPinned ||
            savedSnapshot.imagePaths != currentState.imagePaths ||
            savedSnapshot.backgroundColor != currentState.backgroundColor
    }

    fun hasUnsavedTemplateChanges(
        savedSnapshot: EditorUiState,
        currentState: EditorUiState
    ): Boolean {
        return savedSnapshot.title.trim() != currentState.title.trim() ||
            savedSnapshot.content != currentState.content ||
            savedSnapshot.isMarkdown != currentState.isMarkdown
    }
}
