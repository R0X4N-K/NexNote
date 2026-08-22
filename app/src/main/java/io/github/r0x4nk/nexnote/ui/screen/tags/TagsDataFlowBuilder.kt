package io.github.r0x4nk.nexnote.ui.screen.tags

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesForTagUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchTagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

private const val TAGS_SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun buildTagsFlow(
    searchQuery: Flow<String>,
    sortOrder: Flow<TagSortOrder>,
    observeTagsByUsageDesc: ObserveTagsByUsageDescUseCase,
    observeTagsByUsageAsc: ObserveTagsByUsageAscUseCase,
    observeTagsByDateDesc: ObserveTagsByDateDescUseCase,
    observeTagsByDateAsc: ObserveTagsByDateAscUseCase,
    searchTags: SearchTagsUseCase
): Flow<List<Tag>> {
    return combine(
        searchQuery.debounce(TAGS_SEARCH_DEBOUNCE_MS),
        sortOrder
    ) { query, order -> query to order }
        .flatMapLatest { (query, order) ->
            if (query.isBlank()) {
                buildSortedTagsFlow(
                    order = order,
                    observeTagsByUsageDesc = observeTagsByUsageDesc,
                    observeTagsByUsageAsc = observeTagsByUsageAsc,
                    observeTagsByDateDesc = observeTagsByDateDesc,
                    observeTagsByDateAsc = observeTagsByDateAsc
                )
            } else {
                searchTags(query)
            }
        }
}

private fun buildSortedTagsFlow(
    order: TagSortOrder,
    observeTagsByUsageDesc: ObserveTagsByUsageDescUseCase,
    observeTagsByUsageAsc: ObserveTagsByUsageAscUseCase,
    observeTagsByDateDesc: ObserveTagsByDateDescUseCase,
    observeTagsByDateAsc: ObserveTagsByDateAscUseCase
): Flow<List<Tag>> {
    return when (order) {
        TagSortOrder.USAGE_DESC -> observeTagsByUsageDesc()
        TagSortOrder.USAGE_ASC -> observeTagsByUsageAsc()
        TagSortOrder.DATE_DESC -> observeTagsByDateDesc()
        TagSortOrder.DATE_ASC -> observeTagsByDateAsc()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildNotesForSelectedTagFlow(
    selectedTagName: Flow<String?>,
    observeNotesForTag: ObserveNotesForTagUseCase
): Flow<List<Note>> {
    return selectedTagName.flatMapLatest { tagName ->
        if (tagName == null) {
            flowOf(emptyList())
        } else {
            observeNotesForTag(tagName)
        }
    }
}
