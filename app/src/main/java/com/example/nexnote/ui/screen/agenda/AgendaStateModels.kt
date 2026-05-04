package com.example.nexnote.ui.screen.agenda

import androidx.compose.runtime.Immutable
import com.example.nexnote.domain.model.Note
import com.example.nexnote.ui.common.NoteListViewMode
import com.example.nexnote.ui.common.SortOrder

/**
 * UI state for the agenda screen.
 *
 * displayedMonth uses Calendar's 0-based month convention.
 * daysWithNotes stores startOfDay(device timezone) timestamps.
 */
@Immutable
data class AgendaUiState(
    val displayedYear: Int = 0,
    val displayedMonth: Int = 0,
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val selectedDay: Int = 1,
    val daysWithNotes: Set<Long> = emptySet(),
    val notesForSelectedDate: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST,
    val selectedTagFilters: Set<String> = emptySet(),
    val isLoading: Boolean = true
)
