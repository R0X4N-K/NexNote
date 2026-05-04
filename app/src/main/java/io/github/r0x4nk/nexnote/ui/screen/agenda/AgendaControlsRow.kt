package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder

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
            .padding(horizontal = 4.dp, vertical = 2.dp),
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
    IconButton(onClick = { actions.onSearchToggle(false) }) {
        Icon(Icons.Default.Close, contentDescription = "Close search")
    }

    BasicTextField(
        value = searchQuery,
        onValueChange = actions.onSearchQueryChange,
        modifier = Modifier
            .weight(1f)
            .focusRequester(searchFocusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        decorationBox = { inner ->
            AgendaSearchDecoration(searchQuery = searchQuery, inner = inner)
        }
    )
}

@Composable
private fun AgendaSearchDecoration(
    searchQuery: String,
    inner: @Composable () -> Unit
) {
    Box {
        if (searchQuery.isEmpty()) {
            Text(
                text = "Search today's notes…",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
        }
        inner()
    }
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
    IconButton(onClick = { actions.onSearchToggle(true) }) {
        Icon(Icons.Default.Search, contentDescription = "Search")
    }
}

@Composable
private fun AgendaSortButton(
    sortOrder: SortOrder,
    onToggleSort: () -> Unit
) {
    IconButton(onClick = onToggleSort) {
        Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC) {
                "Sort: newest first"
            } else {
                "Sort: oldest first"
            },
            tint = if (sortOrder == SortOrder.MODIFIED_ASC) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        )
    }
}

@Composable
private fun AgendaViewModeButton(
    viewMode: NoteListViewMode,
    onToggleView: () -> Unit
) {
    IconButton(onClick = onToggleView) {
        Icon(
            imageVector = if (viewMode == NoteListViewMode.LIST) {
                Icons.Default.GridView
            } else {
                Icons.AutoMirrored.Filled.ViewList
            },
            contentDescription = if (viewMode == NoteListViewMode.LIST) {
                "Grid view"
            } else {
                "List view"
            }
        )
    }
}
