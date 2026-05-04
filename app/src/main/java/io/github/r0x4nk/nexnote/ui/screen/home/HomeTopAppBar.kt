package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopAppBar(
    uiState: HomeUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit
) {
    TopAppBar(
        title = {
            HomeTopAppBarTitle(
                uiState = uiState,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        actions = {
            HomeTopAppBarActions(
                uiState = uiState,
                onSearchToggle = onSearchToggle,
                onSortToggle = onSortToggle,
                onViewModeToggle = onViewModeToggle,
                onOpenTrash = onOpenTrash
            )
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun HomeTopAppBarTitle(
    uiState: HomeUiState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            HomeSearchField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                focusRequester = searchFocusRequester
            )
        }
        AnimatedVisibility(
            visible = !uiState.isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun HomeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    NexSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Search notes",
        modifier = Modifier
            .fillMaxWidth(),
        focusRequester = focusRequester,
        textStyle = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun HomeTopAppBarActions(
    uiState: HomeUiState,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit
) {
    if (uiState.isSearchActive) {
        NexIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = "Close search",
            onClick = { onSearchToggle(false) }
        )
    } else {
        HomeBrowsingActions(
            uiState = uiState,
            onSearchToggle = onSearchToggle,
            onSortToggle = onSortToggle,
            onViewModeToggle = onViewModeToggle,
            onOpenTrash = onOpenTrash
        )
    }
}

@Composable
private fun HomeBrowsingActions(
    uiState: HomeUiState,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit
) {
    SortOrderButton(sortOrder = uiState.sortOrder, onClick = onSortToggle)
    ViewModeButton(viewMode = uiState.viewMode, onClick = onViewModeToggle)
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        onClick = { onSearchToggle(true) }
    )
    NexIconButton(
        imageVector = Icons.Default.Delete,
        contentDescription = "Trash",
        onClick = onOpenTrash
    )
}

@Composable
private fun SortOrderButton(sortOrder: SortOrder, onClick: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.SwapVert,
        contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC) {
            "Sort: newest first"
        } else {
            "Sort: oldest first"
        },
        onClick = onClick,
        selected = sortOrder == SortOrder.MODIFIED_ASC
    )
}

@Composable
private fun ViewModeButton(viewMode: NoteListViewMode, onClick: () -> Unit) {
    NexIconButton(
        imageVector = if (viewMode == NoteListViewMode.LIST) {
            Icons.Default.GridView
        } else {
            Icons.AutoMirrored.Filled.ViewList
        },
        contentDescription = if (viewMode == NoteListViewMode.LIST) {
            "Grid view"
        } else {
            "List view"
        },
        onClick = onClick
    )
}
