package com.example.nexnote.ui.screen.home

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.ScoredNote
import com.example.nexnote.domain.usecase.ObserveAllNotesSortedAscUseCase
import com.example.nexnote.domain.usecase.ObserveAllNotesUseCase
import com.example.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import com.example.nexnote.domain.usecase.SearchNotesScoredUseCase
import com.example.nexnote.ui.common.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

internal data class HomeNotesQueryResult(
    val notes: List<Note>,
    val scoredResults: List<ScoredNote>
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildFilteredNoteIdsFlow(
    selectedTagFilters: Flow<Set<String>>,
    observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase?
): Flow<Set<Long>> {
    return selectedTagFilters.flatMapLatest { filters ->
        if (filters.isEmpty() || observeFilteredNoteIds == null) flowOf(emptySet())
        else observeFilteredNoteIds(filters)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildHomeNotesQueryFlow(
    searchQuery: Flow<String>,
    sortOrder: Flow<SortOrder>,
    searchNotesScored: SearchNotesScoredUseCase,
    observeAllNotesSortedAsc: ObserveAllNotesSortedAscUseCase,
    observeAllNotes: ObserveAllNotesUseCase,
    searchDebounceMs: Long
): Flow<HomeNotesQueryResult> {
    return combine(
        debouncedSearchQuery(searchQuery, searchDebounceMs),
        sortOrder
    ) { query, order -> query to order }
        .flatMapLatest { (query, order) ->
            homeNotesQueryResultFlow(
                query = query,
                order = order,
                searchNotesScored = searchNotesScored,
                observeAllNotesSortedAsc = observeAllNotesSortedAsc,
                observeAllNotes = observeAllNotes
            )
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun debouncedSearchQuery(
    searchQuery: Flow<String>,
    searchDebounceMs: Long
): Flow<String> = searchQuery.transformLatest { query ->
    if (query.isEmpty()) emit(query)
    else {
        delay(searchDebounceMs)
        emit(query)
    }
}

private fun homeNotesQueryResultFlow(
    query: String,
    order: SortOrder,
    searchNotesScored: SearchNotesScoredUseCase,
    observeAllNotesSortedAsc: ObserveAllNotesSortedAscUseCase,
    observeAllNotes: ObserveAllNotesUseCase
): Flow<HomeNotesQueryResult> = when {
    query.isNotBlank() ->
        searchNotesScored(query).map { scored ->
            HomeNotesQueryResult(
                notes = scored.map { it.note },
                scoredResults = scored
            )
        }

    order == SortOrder.MODIFIED_ASC ->
        observeAllNotesSortedAsc().map { notes ->
            HomeNotesQueryResult(notes = notes, scoredResults = emptyList())
        }

    else ->
        observeAllNotes().map { notes ->
            HomeNotesQueryResult(notes = notes, scoredResults = emptyList())
        }
}
