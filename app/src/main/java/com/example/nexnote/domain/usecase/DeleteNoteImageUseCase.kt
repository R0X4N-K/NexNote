package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.NoteImageStorage

class DeleteNoteImageUseCase(
    private val storage: NoteImageStorage
) {
    suspend operator fun invoke(relativePath: String): Boolean {
        return storage.deleteImage(relativePath)
    }
}
