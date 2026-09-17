package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.NoteAttachmentStorage
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteAttachmentUseCase

internal class AttachmentUseCases(storage: NoteAttachmentStorage) {
    val copyNoteAttachment = CopyNoteAttachmentUseCase(storage)
}
