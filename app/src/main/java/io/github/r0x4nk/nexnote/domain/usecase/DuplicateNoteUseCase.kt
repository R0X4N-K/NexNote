package io.github.r0x4nk.nexnote.domain.usecase

import kotlinx.coroutines.NonCancellable
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.copyStoredNoteFile
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.util.rewriteMappedPaths
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DuplicateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val imageStorage: NoteImageStorage,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(source: Note): Long = withContext(dispatcher) {
        require(!source.isInVault) {
            "Vault notes must use the explicit Vault duplication path."
        }

        val draft = source.copy(
            id = 0L,
            isDeleted = false,
            deletedDate = null,
            imagePaths = emptyList()
        )
        val copied = LinkedHashMap<String, String>()
        val newNoteId = noteRepository.saveNote(draft)
        try {
            source.imagePaths.filter { it.isNotBlank() }.distinct().forEach { path ->
                copied[path] = imageStorage.copyStoredNoteFile(newNoteId, path) {
                    imageStorage.getImageFile(path).inputStream()
                }
            }
            val duplicate = draft.copy(
                id = newNoteId,
                content = source.content.rewriteMappedPaths(copied),
                imagePaths = source.imagePaths.map { copied[it] ?: it }
            )
            if (duplicate.content != draft.content || duplicate.imagePaths != draft.imagePaths) {
                noteRepository.saveNote(duplicate)
            }
            tagRepository.indexNoteTags(newNoteId, duplicate.content)
            newNoteId
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                try { noteRepository.deleteNotePermanently(newNoteId) }
                catch (cleanup: Throwable) { error.addSuppressed(cleanup) }
                copied.values.forEach { path ->
                    try { imageStorage.deleteImage(path) }
                    catch (cleanup: Throwable) { error.addSuppressed(cleanup) }
                }
            }
            throw error
        }
    }
}
