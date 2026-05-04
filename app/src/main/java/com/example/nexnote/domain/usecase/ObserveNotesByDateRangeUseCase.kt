package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotesByDateRangeUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(startMs: Long, endMs: Long): Flow<List<Note>> {
        return repository.getNotesByDateRange(startMs, endMs)
    }
}
