package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.repository.NoteRepository

class GetNoteByIdUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long): Note? {
        return repository.getNoteById(noteId)
    }
}
