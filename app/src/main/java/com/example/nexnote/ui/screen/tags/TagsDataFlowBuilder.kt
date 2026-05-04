package com.example.nexnote.ui.screen.tags

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.usecase.ObserveAllNotesUseCase
import com.example.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import com.example.nexnote.domain.usecase.SearchTagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
    observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase,
    observeAllNotes: ObserveAllNotesUseCase
): Flow<List<Note>> {
    return selectedTagName.flatMapLatest { tagName ->
        if (tagName == null) {
            flowOf(emptyList())
        } else {
            buildNotesForTagFlow(tagName, observeFilteredNoteIds, observeAllNotes)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun buildNotesForTagFlow(
    tagName: String,
    observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase,
    observeAllNotes: ObserveAllNotesUseCase
): Flow<List<Note>> {
    return observeFilteredNoteIds(setOf(tagName))
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                observeAllNotes().map { notes ->
                    notes.filter { it.id in ids }
                }
            }
        }
}
