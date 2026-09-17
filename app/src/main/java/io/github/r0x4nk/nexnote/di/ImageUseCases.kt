package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteImageFileUseCase

internal class ImageUseCases internal constructor(
    imageStorage: NoteImageStorage
) {
    val copyNoteImageToInternal = CopyNoteImageToInternalUseCase(imageStorage)
    val deleteNoteImage = DeleteNoteImageUseCase(imageStorage)
    val getNoteImageFile = GetNoteImageFileUseCase(imageStorage)
}
