package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import java.io.InputStream

class CopyNoteImageToInternalUseCase(
    private val storage: NoteImageStorage
) {
    suspend operator fun invoke(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String {
        return storage.copyImageToInternal(noteId, openInputStream)
    }
}
