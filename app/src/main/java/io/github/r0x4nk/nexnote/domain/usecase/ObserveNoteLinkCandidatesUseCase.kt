package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveNoteLinkCandidatesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteLinkCandidate>> {
        return repository.noteLinkCandidates
    }
}
