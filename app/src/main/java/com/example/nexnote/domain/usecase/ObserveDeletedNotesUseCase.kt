package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveDeletedNotesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.deletedNotes
    }
}
