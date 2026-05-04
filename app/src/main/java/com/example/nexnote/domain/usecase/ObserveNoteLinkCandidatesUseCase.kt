package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.NoteLinkCandidate
import com.example.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveNoteLinkCandidatesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteLinkCandidate>> {
        return repository.noteLinkCandidates
    }
}
