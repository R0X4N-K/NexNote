package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplatesTopBar(
    uiState: TemplatesUiState,
    searchFocusRequester: FocusRequester,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onStartSelection: () -> Unit
) {
    TopAppBar(
        title = {
            TemplatesTopBarTitle(
                uiState = uiState,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        actions = {
            TemplatesTopBarActions(
                uiState = uiState,
                onSearchToggle = onSearchToggle,
                onToggleSortOrder = onToggleSortOrder,
                onToggleViewMode = onToggleViewMode,
                onStartSelection = onStartSelection
            )
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun TemplatesTopBarTitle(
    uiState: TemplatesUiState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            TemplatesSearchField(
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
                text = "Templates",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun TemplatesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    NexSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Search templates",
        modifier = Modifier
            .fillMaxWidth(),
        focusRequester = focusRequester,
        textStyle = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun TemplatesTopBarActions(
    uiState: TemplatesUiState,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onStartSelection: () -> Unit
) {
    if (uiState.isSearchActive) {
        NexIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = "Close search",
            onClick = { onSearchToggle(false) }
        )
    } else {
        TemplatesDefaultActions(
            uiState = uiState,
            onSearchToggle = onSearchToggle,
            onToggleSortOrder = onToggleSortOrder,
            onToggleViewMode = onToggleViewMode,
            onStartSelection = onStartSelection
        )
    }
}

@Composable
private fun TemplatesDefaultActions(
    uiState: TemplatesUiState,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onStartSelection: () -> Unit
) {
    TemplatesSortButton(uiState.sortOrder, onToggleSortOrder)
    TemplatesViewModeButton(uiState.viewMode, onToggleViewMode)
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        onClick = { onSearchToggle(true) }
    )
    TemplatesOverflowMenu(onStartSelection)
}

@Composable
private fun TemplatesSortButton(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.SwapVert,
        contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC) {
            "Sort: newest first"
        } else {
            "Sort: oldest first"
        },
        onClick = onToggleSortOrder,
        selected = sortOrder == SortOrder.MODIFIED_ASC
    )
}

@Composable
private fun TemplatesViewModeButton(
    viewMode: NoteListViewMode,
    onToggleViewMode: () -> Unit
) {
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
        onClick = onToggleViewMode
    )
}

@Composable
private fun TemplatesOverflowMenu(onStartSelection: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        NexIconButton(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More options",
            onClick = { expanded = true },
            selected = expanded
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Select templates") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onStartSelection()
                }
            )
        }
    }
}
