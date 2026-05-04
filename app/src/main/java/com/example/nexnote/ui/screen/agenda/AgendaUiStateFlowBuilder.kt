package com.example.nexnote.ui.screen.agenda

import com.example.nexnote.domain.model.Note
import com.example.nexnote.ui.common.NoteListViewMode
import com.example.nexnote.ui.common.SortOrder
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
    val processedNotes: Flow<List<Note>>,
    val searchQuery: Flow<String>,
    val isSearchActive: Flow<Boolean>,
    val sortOrder: Flow<SortOrder>,
    val viewMode: Flow<NoteListViewMode>,
    val selectedTagFilters: Flow<Set<String>>
)

private data class AgendaDisplayData(
    val displayedMonth: DisplayedMonth,
    val daysWithNotes: Set<Long>
)

private data class AgendaSelectionData(
    val selectedDate: SelectedDate,
    val notes: List<Note>
)

private data class AgendaSearchData(
    val query: String,
    val isActive: Boolean,
    val sortOrder: SortOrder
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
        combine(flows.searchQuery, flows.isSearchActive, flows.sortOrder, ::AgendaSearchData),
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
        notesForSelectedDate = selection.notes,
        searchQuery = search.query,
        isSearchActive = search.isActive,
        sortOrder = search.sortOrder,
        viewMode = viewFilter.viewMode,
        selectedTagFilters = viewFilter.tagFilters,
        isLoading = false
    )
}
