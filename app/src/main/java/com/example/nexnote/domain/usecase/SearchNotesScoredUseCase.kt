package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.ScoredNote
import com.example.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class SearchNotesScoredUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(query: String): Flow<List<ScoredNote>> {
        return repository.searchNotesScored(query)
    }
}
