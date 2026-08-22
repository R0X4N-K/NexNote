package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.runtime.Immutable
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder

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
    val scoredResults: List<ScoredNote> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val searchSort: NoteSearchSort = NoteSearchSort.RELEVANCE,
    val searchScope: NoteSearchScope = NoteSearchScope.TITLE_AND_CONTENT,
    val pinnedFilter: NotePinnedFilter = NotePinnedFilter.ALL,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST,
    val selectedTagFilters: Set<String> = emptySet(),
    val availableTagNames: Set<String> = emptySet(),
    val isLoading: Boolean = true
) {
    val hasActiveSearchFilters: Boolean
        get() = searchScope != NoteSearchScope.TITLE_AND_CONTENT ||
            pinnedFilter != NotePinnedFilter.ALL ||
            selectedTagFilters.isNotEmpty()
}
