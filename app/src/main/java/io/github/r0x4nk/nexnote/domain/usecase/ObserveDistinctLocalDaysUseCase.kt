package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveDistinctLocalDaysUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<Set<Long>> {
        return repository.distinctLocalDays
    }
}
