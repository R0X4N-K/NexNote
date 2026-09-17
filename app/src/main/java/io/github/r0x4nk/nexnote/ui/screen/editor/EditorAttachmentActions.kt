package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteAttachmentUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.util.AttachmentMarkdown
import io.github.r0x4nk.nexnote.util.insertStandaloneMarkdownBlock
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class EditorAttachmentActions(
    private val uiState: MutableStateFlow<EditorUiState>,
    private val copyAttachment: CopyNoteAttachmentUseCase,
    private val deleteFile: DeleteNoteImageUseCase,
    private val saveDelegate: EditorSaveDelegate,
    private val recordHistory: (EditorContentSnapshot, EditorContentSnapshot) -> Unit,
    private val scope: CoroutineScope,
    private val strings: StringProvider
) {
    private val importMutex = Mutex()

    fun onPicked(fileName: () -> String, openInputStream: () -> InputStream?, insertionOffset: Int?) {
        if (!canInsert()) return
        scope.launch {
            importMutex.withLock {
                if (!canInsert() || !saveDelegate.ensurePersisted()) return@withLock
                val noteId = uiState.value.noteId
                var copiedPath: String? = null
                var ownedByNote = false
                uiState.update { it.copy(isImportingAttachment = true) }
                try {
                    val name = withContext(Dispatchers.IO) { fileName() }
                    val path = copyAttachment(noteId, name, openInputStream)
                    copiedPath = path
                    if (!canInsert() || uiState.value.noteId != noteId) return@withLock
                    // Once linked, finish persistence even if navigation cancels the editor scope.
                    withContext(NonCancellable) {
                        val before = uiState.value.toContentSnapshot()
                        val block = AttachmentMarkdown.format(NoteAttachment(path, name))
                        uiState.update { current ->
                            val insertion = insertStandaloneMarkdownBlock(
                                current.content, block, insertionOffset ?: current.content.length
                            )
                            current.copy(
                                content = insertion.text,
                                imagePaths = current.imagePaths + path,
                                isDirty = true,
                                contentVersion = current.contentVersion + 1,
                                contentSelectionOffset = insertion.cursorOffset
                            )
                        }
                        // Never delete a linked payload after an indeterminate save outcome.
                        ownedByNote = true
                        val after = uiState.value.toContentSnapshot()
                        val saved = saveDelegate.flushPendingChanges()
                        if (!saved && uiState.value.isVaultNote) {
                            uiState.update { current -> current.copy(
                                content = current.content.replace(block, ""),
                                imagePaths = current.imagePaths - path,
                                isDirty = true,
                                contentVersion = current.contentVersion + 1,
                                errorMessage = strings.get(R.string.editor_error_save_attachment)
                            ) }
                            ownedByNote = false
                        } else {
                            // A normal-note save failure retains the file for the existing save retry flow.
                            ownedByNote = true
                            recordHistory(before, after)
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    uiState.update {
                        it.copy(errorMessage = strings.get(R.string.editor_error_attach_file))
                    }
                } finally {
                    uiState.update { it.copy(isImportingAttachment = false) }
                    if (!ownedByNote) withContext(NonCancellable) {
                        copiedPath?.let { path -> runCatchingPreservingCancellation { deleteFile(path) } }
                    }
                }
            }
        }
    }

    private fun canInsert(): Boolean = uiState.value.let {
        !it.isTemplateMode && !it.isReadOnly && !it.isVaultLocked && !it.isLoading
    }
}
