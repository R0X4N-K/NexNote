package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTagUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesForTagUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchTagsUseCase

internal class TagUseCases internal constructor(
    tagRepository: TagRepository
) {
    val observeTagsByUsageDesc = ObserveTagsByUsageDescUseCase(tagRepository)
    val observeTagsByUsageAsc = ObserveTagsByUsageAscUseCase(tagRepository)
    val observeTagsByDateDesc = ObserveTagsByDateDescUseCase(tagRepository)
    val observeTagsByDateAsc = ObserveTagsByDateAscUseCase(tagRepository)
    val observeTagsForNote = ObserveTagsForNoteUseCase(tagRepository)
    val observeMostUsedTags = ObserveMostUsedTagsUseCase(tagRepository)
    val observeFilteredNoteIds = ObserveFilteredNoteIdsUseCase(tagRepository)
    val observeNotesForTag = ObserveNotesForTagUseCase(tagRepository)
    val searchTags = SearchTagsUseCase(tagRepository)
    val indexNoteTags = IndexNoteTagsUseCase(tagRepository)
    val deleteTag = DeleteTagUseCase(tagRepository)
}
