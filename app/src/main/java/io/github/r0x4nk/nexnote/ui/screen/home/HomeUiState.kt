package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.runtime.Immutable
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder

@Immutable
data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val totalNoteCount: Int = 0,
    val hasMoreNotes: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
    val scoredResults: List<ScoredNote> = emptyList(),
    val searchSort: HomeSearchSort = HomeSearchSort.RELEVANCE,
    val searchScope: HomeSearchScope = HomeSearchScope.TITLE_AND_CONTENT,
    val pinnedFilter: HomePinnedFilter = HomePinnedFilter.ALL,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST,
    val showTemplatePicker: Boolean = false,
    val templates: List<Template> = emptyList(),
    val selectedTagFilters: Set<String> = emptySet(),
    val topTags: List<Tag> = emptyList()
) {
    val hasActiveSearchFilters: Boolean
        get() = searchScope != HomeSearchScope.TITLE_AND_CONTENT ||
            pinnedFilter != HomePinnedFilter.ALL ||
            selectedTagFilters.isNotEmpty()
}
