package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository

class SetNotePreviewModeUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long, isPreviewMode: Boolean) {
        repository.setPreviewMode(noteId, isPreviewMode)
    }
}
