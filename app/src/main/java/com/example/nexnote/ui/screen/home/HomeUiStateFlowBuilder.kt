package com.example.nexnote.ui.screen.home

import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.model.Template
import com.example.nexnote.ui.common.NoteListViewMode
import com.example.nexnote.ui.common.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class HomeUiStateFlows(
    val notesData: Flow<HomeNotesQueryResult>,
    val filteredNoteIds: Flow<Set<Long>>,
    val searchQuery: Flow<String>,
    val isSearchActive: Flow<Boolean>,
    val selectedTagFilters: Flow<Set<String>>,
    val sortOrder: Flow<SortOrder>,
    val viewMode: Flow<NoteListViewMode>,
    val templates: Flow<List<Template>>,
    val showTemplatePicker: Flow<Boolean>,
    val topTags: Flow<List<Tag>>
)

internal fun buildHomeUiStateFlow(
    flows: HomeUiStateFlows,
    scope: CoroutineScope
): StateFlow<HomeUiState> {
    return combine(
        buildHomeNotesDataFlow(flows),
        buildHomeSearchDataFlow(flows),
        buildHomeSortViewDataFlow(flows),
        buildHomeTemplatePickerDataFlow(flows),
        flows.topTags
    ) { notesData, searchData, sortViewData, templatePickerData, topTags ->
        buildHomeUiState(
            notesData = notesData,
            searchData = searchData,
            sortViewData = sortViewData,
            templatePickerData = templatePickerData,
            topTags = topTags
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

private fun buildHomeNotesDataFlow(flows: HomeUiStateFlows): Flow<HomeNotesData> {
    return combine(flows.notesData, flows.filteredNoteIds) { noteData, ids ->
        HomeNotesData(noteData.notes, noteData.scoredResults, ids)
    }
}

private fun buildHomeSearchDataFlow(flows: HomeUiStateFlows): Flow<HomeSearchData> {
    return combine(
        flows.searchQuery,
        flows.isSearchActive,
        flows.selectedTagFilters,
        ::HomeSearchData
    )
}

private fun buildHomeSortViewDataFlow(flows: HomeUiStateFlows): Flow<HomeSortViewData> {
    return combine(flows.sortOrder, flows.viewMode, ::HomeSortViewData)
}

private fun buildHomeTemplatePickerDataFlow(
    flows: HomeUiStateFlows
): Flow<HomeTemplatePickerData> {
    return combine(flows.templates, flows.showTemplatePicker, ::HomeTemplatePickerData)
}
