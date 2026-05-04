package io.github.r0x4nk.nexnote.ui.screen.templates

import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TEMPLATES_SEARCH_DEBOUNCE_MS = 300L

internal data class TemplatesExtraState(
    val errorMessage: String? = null,
    val activeDialog: TemplatesDialog = TemplatesDialog.None
)

internal data class TemplatesUiStateFlows(
    val templates: Flow<List<Template>>,
    val searchQuery: Flow<String>,
    val isSearchActive: Flow<Boolean>,
    val sortOrder: Flow<SortOrder>,
    val viewMode: Flow<NoteListViewMode>,
    val extra: Flow<TemplatesExtraState>
)

private data class TemplatesSourceData(
    val templates: List<Template>,
    val query: String,
    val isSearchActive: Boolean
)

private data class TemplatesSortViewData(
    val sortOrder: SortOrder,
    val viewMode: NoteListViewMode
)

@OptIn(FlowPreview::class)
internal fun buildTemplatesUiStateFlow(
    flows: TemplatesUiStateFlows,
    scope: CoroutineScope
): StateFlow<TemplatesUiState> {
    return combine(
        buildTemplatesSourceDataFlow(flows),
        combine(flows.sortOrder, flows.viewMode, ::TemplatesSortViewData),
        flows.extra
    ) { source, sortView, extra ->
        buildTemplatesUiState(source, sortView, extra)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TemplatesUiState()
    )
}

@OptIn(FlowPreview::class)
private fun buildTemplatesSourceDataFlow(
    flows: TemplatesUiStateFlows
): Flow<TemplatesSourceData> {
    return combine(
        flows.templates,
        flows.searchQuery.debounce(TEMPLATES_SEARCH_DEBOUNCE_MS).map { it.trim() },
        flows.isSearchActive,
        ::TemplatesSourceData
    )
}

private fun buildTemplatesUiState(
    source: TemplatesSourceData,
    sortView: TemplatesSortViewData,
    extra: TemplatesExtraState
): TemplatesUiState {
    val filtered = filterTemplatesBySearch(source)
    val sorted = sortTemplates(filtered, sortView.sortOrder)

    return TemplatesUiState(
        predefined = sorted.filter { it.isPredefined },
        custom = sorted.filter { !it.isPredefined },
        isLoading = false,
        errorMessage = extra.errorMessage,
        activeDialog = extra.activeDialog,
        searchQuery = source.query,
        isSearchActive = source.isSearchActive,
        sortOrder = sortView.sortOrder,
        viewMode = sortView.viewMode
    )
}

private fun filterTemplatesBySearch(source: TemplatesSourceData): List<Template> {
    if (!source.isSearchActive || source.query.isBlank()) return source.templates

    return source.templates.filter { template ->
        template.name.contains(source.query, ignoreCase = true) ||
            template.content.contains(source.query, ignoreCase = true)
    }
}

private fun sortTemplates(
    templates: List<Template>,
    sortOrder: SortOrder
): List<Template> {
    return when (sortOrder) {
        SortOrder.MODIFIED_DESC -> templates.sortedByDescending { it.id }
        SortOrder.MODIFIED_ASC -> templates.sortedBy { it.id }
    }
}
