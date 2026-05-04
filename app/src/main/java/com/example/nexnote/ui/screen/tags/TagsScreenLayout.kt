package com.example.nexnote.ui.screen.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagsScreenLayout(
    uiState: TagsUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    searchFocusRequester: FocusRequester,
    isSearchActive: Boolean,
    showSortMenu: Boolean,
    actions: TagsActions
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TagsTopBar(
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                isSearchActive = isSearchActive,
                showSortMenu = showSortMenu,
                actions = actions
            )
        }
    ) { innerPadding ->
        TagsScreenContent(
            uiState = uiState,
            searchFocusRequester = searchFocusRequester,
            isSearchActive = isSearchActive,
            actions = actions,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun TagsScreenContent(
    uiState: TagsUiState,
    searchFocusRequester: FocusRequester,
    isSearchActive: Boolean,
    actions: TagsActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        InlineSearchBar(
            isVisible = isSearchActive,
            query = uiState.searchQuery,
            focusRequester = searchFocusRequester,
            onQueryChange = actions.onSearchQueryChange,
            onClose = actions.onSearchClose
        )
        TagsBody(
            uiState = uiState,
            actions = actions,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TagsBody(
    uiState: TagsUiState,
    actions: TagsActions,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> TagsLoadingState(modifier.fillMaxWidth())
        uiState.tags.isEmpty() -> TagsEmptyState(
            hasSearch = uiState.searchQuery.isNotBlank(),
            modifier = modifier.fillMaxWidth()
        )
        else -> TagsList(
            tags = uiState.tags,
            selectedTagName = uiState.selectedTagName,
            notesForSelectedTag = uiState.notesForSelectedTag,
            actions = actions,
            modifier = modifier
        )
    }
}

@Composable
private fun TagsLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TagsList(
    tags: List<Tag>,
    selectedTagName: String?,
    notesForSelectedTag: List<Note>,
    actions: TagsActions,
    modifier: Modifier = Modifier
) {
    val maxCount = remember(tags) {
        tags.maxOfOrNull { it.noteCount }?.coerceAtLeast(1) ?: 1
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(items = tags, key = { tag -> tag.name }, contentType = { "tag_item" }) { tag ->
            TagsListItem(
                tag = tag,
                maxCount = maxCount,
                selectedTagName = selectedTagName,
                notesForSelectedTag = notesForSelectedTag,
                actions = actions
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun TagsListItem(
    tag: Tag,
    maxCount: Int,
    selectedTagName: String?,
    notesForSelectedTag: List<Note>,
    actions: TagsActions
) {
    TagScoreboardItem(
        tag = tag,
        maxCount = maxCount,
        isExpanded = tag.name == selectedTagName,
        notes = if (tag.name == selectedTagName) notesForSelectedTag else emptyList(),
        onTagClick = { actions.onTagClick(tag.name) },
        onNoteClick = actions.onNoteClick,
        onDeleteClick = { actions.onDeleteClick(tag) }
    )
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}
