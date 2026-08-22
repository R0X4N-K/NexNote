package io.github.r0x4nk.nexnote.ui.screen.home

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.HomeNotesQuery
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.usecase.ObserveHomeNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveHomeNoteIdsUseCase
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.util.SearchUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

internal data class HomeNotesQueryResult(
    val notes: List<Note>,
    val scoredResults: List<ScoredNote>,
    val hasMore: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildHomeNotesQueryFlow(
    searchQuery: Flow<String>,
    isSearchActive: Flow<Boolean>,
    sortOrder: Flow<SortOrder>,
    searchSort: Flow<HomeSearchSort>,
    searchScope: Flow<HomeSearchScope>,
    pinnedFilter: Flow<HomePinnedFilter>,
    selectedTagFilters: Flow<Set<String>>,
    noteLimit: Flow<Int>,
    observeHomeNotes: ObserveHomeNotesUseCase,
    searchDebounceMs: Long
): Flow<HomeNotesQueryResult> {
    val queryAndSearchState = combine(
        debouncedSearchQuery(searchQuery, searchDebounceMs),
        isSearchActive,
        sortOrder,
        searchSort,
        searchScope
    ) { query, searchActive, order, resultSort, scope ->
        HomeQueryPrimaryState(query, searchActive, order, resultSort, scope)
    }
    val filters = combine(
        selectedTagFilters,
        pinnedFilter,
        noteLimit
    ) { tags, pinned, limit -> HomeQueryFilters(tags, pinned, limit) }
    return combine(
        queryAndSearchState,
        filters
    ) { primary, queryFilters ->
        HomeNotesQuery(
            text = primary.query,
            sortAscending = primary.browseSort == SortOrder.MODIFIED_ASC,
            searchSort = primary.resultSort,
            searchScope = if (primary.searchActive) {
                primary.scope
            } else {
                HomeSearchScope.TITLE_AND_CONTENT
            },
            pinnedFilter = if (primary.searchActive) {
                queryFilters.pinned
            } else {
                HomePinnedFilter.ALL
            },
            tagNames = queryFilters.tags,
            limit = queryFilters.limit
        )
    }.flatMapLatest { query ->
        observeHomeNotes(query.copy(limit = query.limit + 1)).map { candidates ->
            val scoredCandidates = if (query.text.isBlank()) {
                emptyList()
            } else if (query.searchSort == HomeSearchSort.RELEVANCE) {
                SearchUtils.scoreAndRank(candidates, query.text, query.searchScope)
            } else {
                SearchUtils.scoreInOrder(candidates, query.text, query.searchScope)
            }
            val scored = scoredCandidates.take(query.limit)
            HomeNotesQueryResult(
                notes = if (query.text.isBlank()) {
                    candidates.take(query.limit)
                } else {
                    scored.map(ScoredNote::note)
                },
                scoredResults = scored,
                hasMore = candidates.size > query.limit
            )
        }
    }
}

private data class HomeQueryPrimaryState(
    val query: String,
    val searchActive: Boolean,
    val browseSort: SortOrder,
    val resultSort: HomeSearchSort,
    val scope: HomeSearchScope
)

/** Observes every matching id while note bodies remain bounded by the Home window. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildHomeSelectionCandidateIdsFlow(
    searchQuery: Flow<String>,
    isSearchActive: Flow<Boolean>,
    searchScope: Flow<HomeSearchScope>,
    pinnedFilter: Flow<HomePinnedFilter>,
    selectedTagFilters: Flow<Set<String>>,
    observeHomeNoteIds: ObserveHomeNoteIdsUseCase,
    searchDebounceMs: Long
): Flow<Set<Long>> {
    return combine(
        debouncedSearchQuery(searchQuery, searchDebounceMs),
        isSearchActive,
        searchScope,
        pinnedFilter,
        selectedTagFilters
    ) { query, searchActive, scope, pinned, tags ->
        HomeNotesQuery(
            text = query.takeIf { searchActive }.orEmpty(),
            searchScope = if (searchActive) scope else HomeSearchScope.TITLE_AND_CONTENT,
            pinnedFilter = if (searchActive) pinned else HomePinnedFilter.ALL,
            tagNames = tags,
            limit = 1
        )
    }
        .distinctUntilChanged()
        .flatMapLatest(observeHomeNoteIds::invoke)
        .map { ids -> ids.toSet() }
}

private data class HomeQueryFilters(
    val tags: Set<String>,
    val pinned: HomePinnedFilter,
    val limit: Int
)

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
