package com.example.nexnote.ui.screen.editor

import com.example.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import com.example.nexnote.domain.usecase.DeleteNoteImageUseCase
import com.example.nexnote.util.insertStandaloneMarkdownBlock
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ADD_TEXT_BEFORE_IMAGE_ERROR = "Add some text before inserting an image"
private const val IMAGE_INSERT_ERROR = "Could not insert image"
private const val MARKDOWN_IMAGE_ALT_TEXT = "image"

internal class EditorImageActions(
    private val uiState: MutableStateFlow<EditorUiState>,
    private val copyNoteImageToInternal: CopyNoteImageToInternalUseCase,
    private val deleteNoteImage: DeleteNoteImageUseCase,
    private val saveDelegate: EditorSaveDelegate,
    private val recordContentHistoryChange: (EditorContentSnapshot, EditorContentSnapshot) -> Unit,
    private val scope: CoroutineScope
) {
    fun onImagePicked(
        openImageInputStream: () -> InputStream?,
        insertionOffset: Int? = null
    ) {
        if (uiState.value.isTemplateMode) return

        scope.launch {
            if (!ensureNoteExistsBeforeImageInsert()) return@launch
            insertImageIntoCurrentNote(openImageInputStream, insertionOffset)
        }
    }

    private suspend fun ensureNoteExistsBeforeImageInsert(): Boolean {
        if (uiState.value.noteId != EditorViewModel.NO_ID) return true

        uiState.update { it.copy(isDirty = true) }
        saveDelegate.performSave()
        if (uiState.value.noteId != EditorViewModel.NO_ID) return true

        uiState.update { it.copy(errorMessage = ADD_TEXT_BEFORE_IMAGE_ERROR) }
        return false
    }

    private suspend fun insertImageIntoCurrentNote(
        openImageInputStream: () -> InputStream?,
        insertionOffset: Int?
    ) {
        val noteId = uiState.value.noteId
        uiState.update { it.copy(isSaving = true) }
        try {
            val relativePath = copyNoteImageToInternal(noteId, openImageInputStream)
            val before = uiState.value.toContentSnapshot()
            var after: EditorContentSnapshot? = null
            uiState.update { current ->
                current.withInsertedImage(relativePath, insertionOffset).also { next ->
                    after = next.toContentSnapshot()
                }
            }
            after?.let { recordContentHistoryChange(before, it) }
            saveDelegate.scheduleAutosave()
        } catch (e: Exception) {
            uiState.update { it.copy(isSaving = false, errorMessage = IMAGE_INSERT_ERROR) }
        }
    }

    private fun EditorUiState.withInsertedImage(
        relativePath: String,
        insertionOffset: Int?
    ): EditorUiState {
        val insertion = insertStandaloneMarkdownBlock(
            text = content,
            block = "![$MARKDOWN_IMAGE_ALT_TEXT]($relativePath)",
            offset = insertionOffset ?: content.length
        )
        return copy(
            content = insertion.text,
            imagePaths = imagePaths + relativePath,
            isDirty = true,
            isSaving = false,
            contentVersion = contentVersion + 1,
            contentSelectionOffset = insertion.cursorOffset
        )
    }

    fun onRemoveImage(relativePath: String) {
        scope.launch {
            deleteNoteImage(relativePath)
            val tagRegex = Regex("""!\[[^\]]*]\(${Regex.escape(relativePath)}\)\n?""")
            val before = uiState.value.toContentSnapshot()
            var after: EditorContentSnapshot? = null
            uiState.update { current ->
                current.copy(
                    content = tagRegex.replace(current.content, ""),
                    imagePaths = current.imagePaths - relativePath,
                    isDirty = true,
                    contentVersion = current.contentVersion + 1,
                    contentSelectionOffset = null
                ).also { next ->
                    after = next.toContentSnapshot()
                }
            }
            after?.let { recordContentHistoryChange(before, it) }
            saveDelegate.scheduleAutosave()
        }
    }
}
