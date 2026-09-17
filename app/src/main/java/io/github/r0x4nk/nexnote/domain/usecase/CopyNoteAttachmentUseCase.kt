package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteAttachmentStorage
import java.io.InputStream

class CopyNoteAttachmentUseCase(private val storage: NoteAttachmentStorage) {
    suspend operator fun invoke(noteId: Long, fileName: String, openInputStream: () -> InputStream?): String =
        storage.copyAttachmentToInternal(noteId, fileName, openInputStream)
}
