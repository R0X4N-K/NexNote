package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.NoteRepository

class SetNotePreviewModeUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long, isPreviewMode: Boolean) {
        repository.setPreviewMode(noteId, isPreviewMode)
    }
}
