package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveDistinctLocalDaysUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<Set<Long>> {
        return repository.distinctLocalDays
    }
}
