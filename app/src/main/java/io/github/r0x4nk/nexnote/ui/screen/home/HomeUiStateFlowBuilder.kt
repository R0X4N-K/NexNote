package io.github.r0x4nk.nexnote.ui.screen.home

import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class HomeUiStateFlows(
    val notesData: Flow<HomeNotesQueryResult>,
    val activeNoteCount: Flow<Int>,
    val searchQuery: Flow<String>,
    val isSearchActive: Flow<Boolean>,
    val selectedTagFilters: Flow<Set<String>>,
    val searchSort: Flow<HomeSearchSort>,
    val searchScope: Flow<HomeSearchScope>,
    val pinnedFilter: Flow<HomePinnedFilter>,
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
    return combine(flows.notesData, flows.activeNoteCount) { noteData, totalCount ->
        HomeNotesData(
            allNotes = noteData.notes,
            allScored = noteData.scoredResults,
            totalNoteCount = totalCount,
            hasMore = noteData.hasMore
        )
    }
}

private fun buildHomeSearchDataFlow(flows: HomeUiStateFlows): Flow<HomeSearchData> {
    val queryState = combine(
        flows.searchQuery,
        flows.isSearchActive,
        flows.selectedTagFilters
    ) { query, active, tags -> Triple(query, active, tags) }
    val controls = combine(
        flows.searchSort,
        flows.searchScope,
        flows.pinnedFilter
    ) { sort, scope, pinned -> Triple(sort, scope, pinned) }
    return combine(queryState, controls) { query, control ->
        HomeSearchData(
            query = query.first,
            isActive = query.second,
            selectedTagFilters = query.third,
            resultSort = control.first,
            scope = control.second,
            pinnedFilter = control.third
        )
    }
}

private fun buildHomeSortViewDataFlow(flows: HomeUiStateFlows): Flow<HomeSortViewData> {
    return combine(flows.sortOrder, flows.viewMode, ::HomeSortViewData)
}

private fun buildHomeTemplatePickerDataFlow(
    flows: HomeUiStateFlows
): Flow<HomeTemplatePickerData> {
    return combine(flows.templates, flows.showTemplatePicker, ::HomeTemplatePickerData)
}
