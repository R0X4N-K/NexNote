package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveTagsByDateAscUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(): Flow<List<Tag>> {
        return repository.getAllTagsByDateAsc()
    }
}
