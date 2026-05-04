package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage

class DeleteNoteImageUseCase(
    private val storage: NoteImageStorage
) {
    suspend operator fun invoke(relativePath: String): Boolean {
        return storage.deleteImage(relativePath)
    }
}
