package io.github.r0x4nk.nexnote.ui.screen.agenda

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class AgendaUiStateFlows(
    val displayedMonth: Flow<DisplayedMonth>,
    val daysWithNotes: Flow<Set<Long>>,
    val selectedDate: Flow<SelectedDate>,
    val processedNotes: Flow<AgendaProcessedNotes>,
    val searchQuery: Flow<String>,
    val isSearchActive: Flow<Boolean>,
    val sortOrder: Flow<SortOrder>,
    val searchSort: Flow<NoteSearchSort>,
    val searchScope: Flow<NoteSearchScope>,
    val pinnedFilter: Flow<NotePinnedFilter>,
    val viewMode: Flow<NoteListViewMode>,
    val selectedTagFilters: Flow<Set<String>>
)

private data class AgendaDisplayData(
    val displayedMonth: DisplayedMonth,
    val daysWithNotes: Set<Long>
)

private data class AgendaSelectionData(
    val selectedDate: SelectedDate,
    val processedNotes: AgendaProcessedNotes
)

private data class AgendaSearchData(
    val query: String,
    val isActive: Boolean,
    val sortOrder: SortOrder,
    val resultSort: NoteSearchSort,
    val scope: NoteSearchScope,
    val pinnedFilter: NotePinnedFilter
)

private data class AgendaViewFilterData(
    val viewMode: NoteListViewMode,
    val tagFilters: Set<String>
)

internal fun buildAgendaUiStateFlow(
    flows: AgendaUiStateFlows,
    scope: CoroutineScope
): StateFlow<AgendaUiState> {
    return combine(
        combine(flows.displayedMonth, flows.daysWithNotes, ::AgendaDisplayData),
        combine(flows.selectedDate, flows.processedNotes, ::AgendaSelectionData),
        combine(
            combine(flows.searchQuery, flows.isSearchActive, flows.sortOrder) { query, active, order ->
                AgendaPrimarySearchData(query, active, order)
            },
            combine(flows.searchSort, flows.searchScope, flows.pinnedFilter) { sort, scope, pinned ->
                AgendaAdvancedSearchData(sort, scope, pinned)
            }
        ) { primary, advanced ->
            AgendaSearchData(
                query = primary.query,
                isActive = primary.isActive,
                sortOrder = primary.sortOrder,
                resultSort = advanced.resultSort,
                scope = advanced.scope,
                pinnedFilter = advanced.pinnedFilter
            )
        },
        combine(flows.viewMode, flows.selectedTagFilters, ::AgendaViewFilterData)
    ) { display, selection, search, viewFilter ->
        buildAgendaUiState(display, selection, search, viewFilter)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AgendaUiState()
    )
}

private fun buildAgendaUiState(
    display: AgendaDisplayData,
    selection: AgendaSelectionData,
    search: AgendaSearchData,
    viewFilter: AgendaViewFilterData
): AgendaUiState {
    return AgendaUiState(
        displayedYear = display.displayedMonth.year,
        displayedMonth = display.displayedMonth.month,
        selectedYear = selection.selectedDate.year,
        selectedMonth = selection.selectedDate.month,
        selectedDay = selection.selectedDate.day,
        daysWithNotes = display.daysWithNotes,
        notesForSelectedDate = selection.processedNotes.notes,
        scoredResults = selection.processedNotes.scoredResults,
        searchQuery = search.query,
        isSearchActive = search.isActive,
        sortOrder = search.sortOrder,
        searchSort = search.resultSort,
        searchScope = search.scope,
        pinnedFilter = search.pinnedFilter,
        viewMode = viewFilter.viewMode,
        selectedTagFilters = viewFilter.tagFilters,
        availableTagNames = selection.processedNotes.availableTagNames,
        isLoading = false
    )
}

private data class AgendaPrimarySearchData(
    val query: String,
    val isActive: Boolean,
    val sortOrder: SortOrder
)

private data class AgendaAdvancedSearchData(
    val resultSort: NoteSearchSort,
    val scope: NoteSearchScope,
    val pinnedFilter: NotePinnedFilter
)
