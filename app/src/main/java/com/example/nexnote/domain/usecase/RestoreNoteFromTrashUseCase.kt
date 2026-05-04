package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.NoteRepository

class RestoreNoteFromTrashUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long) {
        repository.restoreFromTrash(noteId)
    }
}
