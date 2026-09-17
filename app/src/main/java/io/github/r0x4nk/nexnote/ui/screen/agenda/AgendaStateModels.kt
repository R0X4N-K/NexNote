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
 * Fixed buckets of the Agenda timeline, ordered from the most recent to the
 * oldest. The first four are relative to "now"; notes older than a month are
 * grouped by calendar month for the last year and by year before that.
 */
enum class AgendaSectionKind {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    MONTH,
    YEAR
}

/**
 * A section of the Agenda timeline: every note whose creation day falls inside
 * the same relative or calendar bucket, ordered by the active sort and search.
 * Items keep their search highlight ranges so the timeline can match the
 * calendar surface.
 *
 * [key] is stable across recompositions and is used to persist section
 * expansion; [month] follows Calendar's 0-based convention.
 */
@Immutable
data class AgendaTimelineSection(
    val key: String,
    val kind: AgendaSectionKind,
    val year: Int = 0,
    val month: Int = 0,
    val items: List<ScoredNote>
) {
    val noteCount: Int get() = items.size
    val notes: List<Note> get() = items.map { item -> item.note }
}

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
    val timelineGroups: List<AgendaTimelineSection> = emptyList(),
    val scoredResults: List<ScoredNote> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val appliedSortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val appliedSearchSort: NoteSearchSort = NoteSearchSort.RELEVANCE,
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

    val timelineNoteCount: Int get() = timelineGroups.sumOf { group -> group.noteCount }
}
