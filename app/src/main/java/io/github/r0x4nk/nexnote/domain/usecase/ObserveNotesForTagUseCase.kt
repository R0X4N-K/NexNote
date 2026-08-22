package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

/** Observes active normal notes associated with one tag. */
class ObserveNotesForTagUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(tagName: String): Flow<List<Note>> =
        repository.observeNotesForTag(tagName)
}
