package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveMostUsedTagsUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(limit: Int = TagRepository.DEFAULT_TOP_TAGS_LIMIT): Flow<List<Tag>> {
        return repository.getMostUsedTags(limit)
    }
}
