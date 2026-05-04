package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveTagsByDateAscUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(): Flow<List<Tag>> {
        return repository.getAllTagsByDateAsc()
    }
}
