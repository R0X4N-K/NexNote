package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField

@Composable
internal fun AgendaControlsRow(
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    isSearchActive: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    actions: AgendaActions
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchActive) {
            AgendaSearchControls(
                searchQuery = searchQuery,
                searchFocusRequester = searchFocusRequester,
                actions = actions
            )
        } else {
            AgendaToolbarControls(
                sortOrder = sortOrder,
                viewMode = viewMode,
                actions = actions
            )
        }
    }
}

@Composable
private fun RowScope.AgendaSearchControls(
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    actions: AgendaActions
) {
    NexIconButton(
        imageVector = Icons.Default.Close,
        contentDescription = "Close search",
        onClick = { actions.onSearchToggle(false) }
    )

    NexSearchField(
        value = searchQuery,
        onValueChange = actions.onSearchQueryChange,
        placeholder = "Search this day",
        modifier = Modifier
            .weight(1f),
        focusRequester = searchFocusRequester,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun RowScope.AgendaToolbarControls(
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    actions: AgendaActions
) {
    AgendaSortButton(sortOrder = sortOrder, onToggleSort = actions.onToggleSort)
    AgendaViewModeButton(viewMode = viewMode, onToggleView = actions.onToggleView)
    Spacer(Modifier.weight(1f))
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        onClick = { actions.onSearchToggle(true) }
    )
}

@Composable
private fun AgendaSortButton(
    sortOrder: SortOrder,
    onToggleSort: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.SwapVert,
        contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC) {
            "Sort: newest first"
        } else {
            "Sort: oldest first"
        },
        onClick = onToggleSort,
        selected = sortOrder == SortOrder.MODIFIED_ASC
    )
}

@Composable
private fun AgendaViewModeButton(
    viewMode: NoteListViewMode,
    onToggleView: () -> Unit
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
        onClick = onToggleView
    )
}
