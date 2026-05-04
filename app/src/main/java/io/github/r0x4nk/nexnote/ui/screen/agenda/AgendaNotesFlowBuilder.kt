package io.github.r0x4nk.nexnote.ui.screen.agenda

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.util.DateUtils
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
    observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase?
): Flow<Set<Long>> {
    return selectedTagFilters.flatMapLatest { filters ->
        if (filters.isEmpty() || observeFilteredNoteIds == null) flowOf(emptySet())
        else observeFilteredNoteIds(filters)
    }
}

@OptIn(FlowPreview::class)
internal fun buildAgendaProcessedNotesFlow(
    rawNotesForDay: Flow<List<Note>>,
    filteredNoteIds: Flow<Set<Long>>,
    searchQuery: Flow<String>,
    sortOrder: Flow<SortOrder>,
    searchDebounceMs: Long
): Flow<List<Note>> {
    return combine(
        rawNotesForDay,
        filteredNoteIds,
        searchQuery.debounce(searchDebounceMs),
        sortOrder
    ) { notes, tagIds, query, order ->
        val tagFiltered = if (tagIds.isEmpty()) notes else notes.filter { it.id in tagIds }
        val searched = if (query.isBlank()) {
            tagFiltered
        } else {
            tagFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }
        when (order) {
            SortOrder.MODIFIED_DESC -> searched.sortedByDescending { it.lastModifiedDate }
            SortOrder.MODIFIED_ASC -> searched.sortedBy { it.lastModifiedDate }
        }
    }
}
