package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class SearchNotesScoredUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(query: String): Flow<List<ScoredNote>> {
        return repository.searchNotesScored(query)
    }
}
