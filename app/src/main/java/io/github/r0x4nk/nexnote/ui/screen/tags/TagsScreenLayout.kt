package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagsScreenLayout(
    uiState: TagsUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState,
    searchFocusRequester: FocusRequester,
    isSearchActive: Boolean,
    showSortMenu: Boolean,
    floatingBottomPadding: Dp,
    actions: TagsActions
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // Material 3 "lift the FAB" snackbar host — the radial FAB animates
        // up while a snackbar is visible. See [RadialMenuSnackbarHost].
        snackbarHost = {
            RadialMenuSnackbarHost(
                hostState = snackbarHostState,
                bottomInset = floatingBottomPadding
            )
        },
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
            viewMode = uiState.viewMode,
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
    viewMode: TagsViewMode,
    actions: TagsActions,
    modifier: Modifier = Modifier
) {
    val maxCount = remember(tags) {
        tags.maxOfOrNull { it.noteCount }?.coerceAtLeast(1) ?: 1
    }

    when (viewMode) {
        TagsViewMode.LIST -> TagsScoreboardList(
            tags = tags,
            maxCount = maxCount,
            selectedTagName = selectedTagName,
            notesForSelectedTag = notesForSelectedTag,
            actions = actions,
            modifier = modifier
        )
        TagsViewMode.TREEMAP -> TagsTreemapList(
            tags = tags,
            maxCount = maxCount,
            selectedTagName = selectedTagName,
            notesForSelectedTag = notesForSelectedTag,
            actions = actions,
            modifier = modifier
        )
    }
}

@Composable
private fun TagsScoreboardList(
    tags: List<Tag>,
    maxCount: Int,
    selectedTagName: String?,
    notesForSelectedTag: List<Note>,
    actions: TagsActions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
        onRequestNoteActions = actions.onRequestNoteActions,
        onDeleteClick = { actions.onDeleteClick(tag) }
    )
}
