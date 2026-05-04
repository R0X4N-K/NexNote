package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllNotesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.allNotes
    }
}
