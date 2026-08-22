package io.github.r0x4nk.nexnote.ui.screen.agenda

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.util.DateUtils
import io.github.r0x4nk.nexnote.util.SearchUtils
import io.github.r0x4nk.nexnote.util.TagParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

internal fun buildAgendaDaysWithNotesFlow(
    observeDistinctLocalDays: ObserveDistinctLocalDaysUseCase,
    scope: CoroutineScope
): StateFlow<Set<Long>> {
    return observeDistinctLocalDays().stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet()
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildAgendaRawNotesForDayFlow(
    selectedDate: Flow<SelectedDate>,
    observeNotesByDateRange: ObserveNotesByDateRangeUseCase
): Flow<List<Note>> {
    return selectedDate.flatMapLatest { date ->
        val noon = DateUtils.toMillis(date.year, date.month, date.day)
        observeNotesByDateRange(
            startMs = DateUtils.startOfDay(noon),
            endMs = DateUtils.startOfNextDay(noon)
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildAgendaFilteredNoteIdsFlow(
    selectedTagFilters: Flow<Set<String>>,
    observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase
): Flow<Set<Long>> {
    return selectedTagFilters.flatMapLatest { filters ->
        if (filters.isEmpty()) flowOf(emptySet())
        else observeFilteredNoteIds(filters)
    }
}

@OptIn(FlowPreview::class)
internal fun buildAgendaProcessedNotesFlow(
    rawNotesForDay: Flow<List<Note>>,
    filteredNoteIds: Flow<Set<Long>>,
    searchQuery: Flow<String>,
    sortOrder: Flow<SortOrder>,
    searchSort: Flow<NoteSearchSort>,
    searchScope: Flow<NoteSearchScope>,
    pinnedFilter: Flow<NotePinnedFilter>,
    searchDebounceMs: Long
): Flow<AgendaProcessedNotes> {
    val searchOptions = combine(
        searchQuery.debounce(searchDebounceMs),
        searchSort,
        searchScope,
        pinnedFilter,
        ::AgendaSearchOptions
    )
    return combine(
        rawNotesForDay,
        filteredNoteIds,
        sortOrder,
        searchOptions
    ) { notes, tagIds, order, options ->
        val availableTagNames = notes.asSequence()
            .flatMap { note -> TagParser.extractTags(note.content).asSequence() }
            .toSet()
        val tagFiltered = if (tagIds.isEmpty()) notes else notes.filter { it.id in tagIds }
        val pinnedFiltered = tagFiltered.filter { note ->
            when (options.pinnedFilter) {
                NotePinnedFilter.ALL -> true
                NotePinnedFilter.PINNED -> note.isPinned
                NotePinnedFilter.UNPINNED -> !note.isPinned
            }
        }
        if (options.query.isBlank()) {
            return@combine AgendaProcessedNotes(
                notes = pinnedFiltered.sortedByPinnedAndModifiedDate(order),
                availableTagNames = availableTagNames
            )
        }

        val scored = SearchUtils.searchAndSort(
            notes = pinnedFiltered,
            query = options.query,
            scope = options.searchScope,
            sort = options.searchSort
        )
        AgendaProcessedNotes(
            notes = scored.map(ScoredNote::note),
            scoredResults = scored,
            availableTagNames = availableTagNames
        )
    }
}

internal data class AgendaProcessedNotes(
    val notes: List<Note>,
    val scoredResults: List<ScoredNote> = emptyList(),
    val availableTagNames: Set<String> = emptySet()
)

private data class AgendaSearchOptions(
    val query: String,
    val searchSort: NoteSearchSort,
    val searchScope: NoteSearchScope,
    val pinnedFilter: NotePinnedFilter
)

private fun List<Note>.sortedByPinnedAndModifiedDate(order: SortOrder): List<Note> {
    val comparator = when (order) {
        SortOrder.MODIFIED_DESC ->
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.lastModifiedDate }
                .thenBy { it.id }

        SortOrder.MODIFIED_ASC ->
            compareByDescending<Note> { it.isPinned }
                .thenBy { it.lastModifiedDate }
                .thenBy { it.id }
    }

    return sortedWith(comparator)
}
