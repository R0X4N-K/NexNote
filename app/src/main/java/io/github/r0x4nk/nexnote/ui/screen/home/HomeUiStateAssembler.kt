package io.github.r0x4nk.nexnote.ui.screen.home

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder

internal data class HomeNotesData(
    val allNotes: List<Note>,
    val allScored: List<ScoredNote>,
    val filteredIds: Set<Long>
)

internal data class HomeSearchData(
    val query: String,
    val isActive: Boolean,
    val selectedTagFilters: Set<String>
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
    val notes = filterNotesByIds(notesData.allNotes, notesData.filteredIds)
    val scored = filterScoredByIds(notesData.allScored, notesData.filteredIds)

    return HomeUiState(
        notes = notes,
        searchQuery = searchData.query,
        isSearchActive = searchData.isActive,
        isLoading = false,
        scoredResults = scored,
        sortOrder = sortViewData.sortOrder,
        viewMode = sortViewData.viewMode,
        showTemplatePicker = templatePickerData.showPicker,
        templates = templatePickerData.templates,
        selectedTagFilters = searchData.selectedTagFilters,
        topTags = topTags
    )
}

private fun filterNotesByIds(notes: List<Note>, filteredIds: Set<Long>): List<Note> {
    return if (filteredIds.isEmpty()) notes else notes.filter { it.id in filteredIds }
}

private fun filterScoredByIds(
    scoredResults: List<ScoredNote>,
    filteredIds: Set<Long>
): List<ScoredNote> {
    return if (filteredIds.isEmpty()) scoredResults
    else scoredResults.filter { it.note.id in filteredIds }
}
