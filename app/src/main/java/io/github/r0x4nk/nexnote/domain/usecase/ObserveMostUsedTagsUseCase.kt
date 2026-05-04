package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveMostUsedTagsUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(limit: Int = TagRepository.DEFAULT_TOP_TAGS_LIMIT): Flow<List<Tag>> {
        return repository.getMostUsedTags(limit)
    }
}
