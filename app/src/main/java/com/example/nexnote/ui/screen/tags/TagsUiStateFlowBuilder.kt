package com.example.nexnote.ui.screen.tags

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class TagsUiStateFlows(
    val tags: Flow<List<Tag>>,
    val searchQuery: Flow<String>,
    val sortOrder: Flow<TagSortOrder>,
    val selectedTagName: Flow<String?>,
    val notesForSelectedTag: Flow<List<Note>>,
    val activeDialog: Flow<TagsDialog>
)

private data class TagsListData(
    val tags: List<Tag>,
    val searchQuery: String,
    val sortOrder: TagSortOrder
)

private data class TagsSelectionData(
    val selectedTagName: String?,
    val notes: List<Note>
)

internal fun buildTagsUiStateFlow(
    flows: TagsUiStateFlows,
    scope: CoroutineScope
): StateFlow<TagsUiState> {
    return combine(
        combine(flows.tags, flows.searchQuery, flows.sortOrder, ::TagsListData),
        combine(flows.selectedTagName, flows.notesForSelectedTag, ::TagsSelectionData),
        flows.activeDialog
    ) { listData, selectionData, dialog ->
        TagsUiState(
            tags = listData.tags,
            searchQuery = listData.searchQuery,
            sortOrder = listData.sortOrder,
            selectedTagName = selectionData.selectedTagName,
            notesForSelectedTag = selectionData.notes,
            isLoading = false,
            activeDialog = dialog
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagsUiState()
    )
}
