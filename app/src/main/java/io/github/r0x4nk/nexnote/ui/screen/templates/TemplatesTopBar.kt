package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu
import io.github.r0x4nk.nexnote.ui.component.NoteListSortButton
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
            Column {
                Text(
                    text = stringResource(R.string.templates_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.templates_starting_points,
                        uiState.predefined.size + uiState.custom.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        placeholder = stringResource(R.string.home_search_templates),
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
            contentDescription = stringResource(R.string.common_close_search),
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
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.common_search),
        onClick = { onSearchToggle(true) }
    )
    NoteListSortButton(
        sortOrder = uiState.sortOrder,
        onToggleSortOrder = onToggleSortOrder
    )
    TemplatesOverflowMenu(
        uiState = uiState,
        onToggleViewMode = onToggleViewMode,
        onStartSelection = onStartSelection
    )
}

@Composable
private fun TemplatesOverflowMenu(
    uiState: TemplatesUiState,
    onToggleViewMode: () -> Unit,
    onStartSelection: () -> Unit
) {
    NoteListOverflowMenu(
        viewMode = uiState.viewMode,
        onToggleViewMode = onToggleViewMode,
        availableViewModes = NoteListViewMode.listGridModes
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.templates_select)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                onStartSelection()
            }
        )
    }
}
