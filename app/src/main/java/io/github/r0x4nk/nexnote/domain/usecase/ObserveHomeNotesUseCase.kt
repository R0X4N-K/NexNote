package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.HomeNotesQuery
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveHomeNotesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(query: HomeNotesQuery): Flow<List<Note>> =
        repository.observeHomeNotes(query)
}

class ObserveHomeNoteIdsUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(query: HomeNotesQuery): Flow<List<Long>> =
        repository.observeHomeNoteIds(query)
}

class ObserveActiveNoteCountUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<Int> = repository.activeNoteCount
}
