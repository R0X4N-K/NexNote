package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.NoteImageStorage
import java.io.File

class GetNoteImageFileUseCase(
    private val storage: NoteImageStorage
) {
    operator fun invoke(relativePath: String): File {
        return storage.getImageFile(relativePath)
    }
}
