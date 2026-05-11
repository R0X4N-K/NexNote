package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.util.insertStandaloneMarkdownBlock
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    /**
     * Guarantees the note has a database id before the image file is copied
     * to internal storage.
     *
     * Image storage namespaces files under the owning note id, so a brand-new
     * note must be persisted first — even when the user has not typed a title
     * or body. We delegate to [EditorSaveDelegate.ensurePersisted], which
     * intentionally bypasses the autosave "non-empty" guard for this exact
     * case. The image insertion itself is the user's first content gesture
     * and must not be blocked by it.
     *
     * Returns `false` only when the underlying save genuinely failed, in
     * which case [EditorSaveDelegate] has already published a user-facing
     * error message.
     */
    private suspend fun ensureNoteExistsBeforeImageInsert(): Boolean {
        if (uiState.value.noteId != EditorViewModel.NO_ID) return true

        val persisted = saveDelegate.ensurePersisted()
        return persisted && uiState.value.noteId != EditorViewModel.NO_ID
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
