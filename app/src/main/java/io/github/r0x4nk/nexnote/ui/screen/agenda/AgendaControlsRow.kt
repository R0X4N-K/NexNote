package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu

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
    Spacer(Modifier.weight(1f))
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        onClick = { actions.onSearchToggle(true) }
    )
    AgendaOverflowMenu(
        sortOrder = sortOrder,
        viewMode = viewMode,
        actions = actions
    )
}

@Composable
private fun AgendaOverflowMenu(
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    actions: AgendaActions
) {
    NoteListOverflowMenu(
        sortOrder = sortOrder,
        viewMode = viewMode,
        onToggleSortOrder = actions.onToggleSort,
        onToggleViewMode = actions.onToggleView
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text("Select notes") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                actions.onStartNoteSelection()
            }
        )
    }
}
