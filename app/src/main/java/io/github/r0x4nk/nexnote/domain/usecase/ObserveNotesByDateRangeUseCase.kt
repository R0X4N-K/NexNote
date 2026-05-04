package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotesByDateRangeUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(startMs: Long, endMs: Long): Flow<List<Note>> {
        return repository.getNotesByDateRange(startMs, endMs)
    }
}
