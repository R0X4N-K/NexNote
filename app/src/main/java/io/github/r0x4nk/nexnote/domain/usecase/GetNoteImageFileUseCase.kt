package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import java.io.File

class GetNoteImageFileUseCase(
    private val storage: NoteImageStorage
) {
    operator fun invoke(relativePath: String): File {
        return storage.getImageFile(relativePath)
    }
}
