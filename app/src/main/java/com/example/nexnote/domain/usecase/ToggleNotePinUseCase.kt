package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.repository.NoteRepository

class ToggleNotePinUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        repository.setPinned(note.id, !note.isPinned)
    }
}
