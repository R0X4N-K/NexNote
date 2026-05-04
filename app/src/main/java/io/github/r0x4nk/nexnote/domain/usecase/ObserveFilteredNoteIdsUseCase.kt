package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveFilteredNoteIdsUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(tagNames: Set<String>): Flow<Set<Long>> {
        return repository.getFilteredNoteIds(tagNames)
    }
}
