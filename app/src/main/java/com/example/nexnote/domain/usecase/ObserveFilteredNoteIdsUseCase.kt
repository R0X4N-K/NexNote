package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveFilteredNoteIdsUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(tagNames: Set<String>): Flow<Set<Long>> {
        return repository.getFilteredNoteIds(tagNames)
    }
}
