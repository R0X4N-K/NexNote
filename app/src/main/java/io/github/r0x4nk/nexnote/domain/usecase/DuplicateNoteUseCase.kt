package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.CancellationException

class DuplicateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val imageStorage: NoteImageStorage
) {
    suspend operator fun invoke(source: Note): Long {
        require(!source.isInVault) {
            "Vault notes must use the explicit Vault duplication path."
        }

        val draft = source.copy(
            id = 0L,
            isDeleted = false,
            deletedDate = null
        )
        val newNoteId = noteRepository.saveNote(draft)
        val imagePathMap = copyImagePaths(newNoteId, source.imagePaths)
        val duplicate = draft.copy(
            id = newNoteId,
            content = draft.content.replaceImagePaths(imagePathMap),
            imagePaths = draft.imagePaths.map { imagePathMap[it] ?: it }
        )

        if (duplicate.content != draft.content || duplicate.imagePaths != draft.imagePaths) {
            noteRepository.saveNote(duplicate)
        }
        tagRepository.indexNoteTags(newNoteId, duplicate.content)
        return newNoteId
    }

    private suspend fun copyImagePaths(
        newNoteId: Long,
        sourcePaths: List<String>
    ): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        sourcePaths
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { sourcePath ->
                result[sourcePath] = copyImagePath(newNoteId, sourcePath)
            }
        return result
    }

    private suspend fun copyImagePath(newNoteId: Long, sourcePath: String): String =
        try {
            imageStorage.copyImageToInternal(newNoteId) {
                imageStorage.getImageFile(sourcePath).inputStream()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            sourcePath
        }

    private fun String.replaceImagePaths(pathMap: Map<String, String>): String {
        if (pathMap.isEmpty()) return this
        var updated = this
        pathMap.forEach { (sourcePath, duplicatePath) ->
            if (sourcePath != duplicatePath) {
                updated = updated.replace(sourcePath, duplicatePath)
            }
        }
        return updated
    }
}
