package io.github.r0x4nk.nexnote.ui.screen.home

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder

internal data class HomeNotesData(
    val allNotes: List<Note>,
    val allScored: List<ScoredNote>,
    val totalNoteCount: Int,
    val hasMore: Boolean
)

internal data class HomeSearchData(
    val query: String,
    val isActive: Boolean,
    val selectedTagFilters: Set<String>,
    val resultSort: HomeSearchSort,
    val scope: HomeSearchScope,
    val pinnedFilter: HomePinnedFilter
)

internal data class HomeSortViewData(
    val sortOrder: SortOrder,
    val viewMode: NoteListViewMode
)

internal data class HomeTemplatePickerData(
    val templates: List<Template>,
    val showPicker: Boolean
)

internal fun buildHomeUiState(
    notesData: HomeNotesData,
    searchData: HomeSearchData,
    sortViewData: HomeSortViewData,
    templatePickerData: HomeTemplatePickerData,
    topTags: List<Tag>
): HomeUiState {
    return HomeUiState(
        notes = notesData.allNotes,
        totalNoteCount = notesData.totalNoteCount,
        hasMoreNotes = notesData.hasMore,
        searchQuery = searchData.query,
        isSearchActive = searchData.isActive,
        isLoading = false,
        scoredResults = notesData.allScored,
        searchSort = searchData.resultSort,
        searchScope = searchData.scope,
        pinnedFilter = searchData.pinnedFilter,
        sortOrder = sortViewData.sortOrder,
        viewMode = sortViewData.viewMode,
        showTemplatePicker = templatePickerData.showPicker,
        templates = templatePickerData.templates,
        selectedTagFilters = searchData.selectedTagFilters,
        topTags = topTags
    )
}
