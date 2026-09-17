package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu
import io.github.r0x4nk.nexnote.ui.component.TagFolderExpandAllButton

/**
 * Controls bar for the Agenda timeline, mirroring the calendar surface:
 * search, sort and the overflow menu, plus the day collapse/expand-all toggle.
 */
@Composable
internal fun AgendaTimelineControlsRow(
    isSearchActive: Boolean,
    searchQuery: String,
    searchSort: NoteSearchSort,
    hasActiveSearchFilters: Boolean,
    isAllSectionsCollapsed: Boolean,
    noteCount: Int,
    searchFocusRequester: FocusRequester,
    onToggleAllSections: () -> Unit,
    actions: AgendaActions,
    modifier: Modifier = Modifier
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
                searchSort = searchSort,
                hasActiveSearchFilters = hasActiveSearchFilters,
                searchFocusRequester = searchFocusRequester,
                placeholder = stringResource(R.string.agenda_search_all),
                actions = actions
            )
        } else {
            AgendaTimelineToolbarControls(
                isAllSectionsCollapsed = isAllSectionsCollapsed,
                noteCount = noteCount,
                onToggleAllSections = onToggleAllSections,
                actions = actions
            )
        }
    }
}

@Composable
private fun RowScope.AgendaTimelineToolbarControls(
    isAllSectionsCollapsed: Boolean,
    noteCount: Int,
    onToggleAllSections: () -> Unit,
    actions: AgendaActions
) {
    Text(
        text = pluralStringResource(R.plurals.home_note_count, noteCount, noteCount),
        modifier = Modifier
            .weight(1f)
            .padding(start = 4.dp, end = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    TagFolderExpandAllButton(
        isAllCollapsed = isAllSectionsCollapsed,
        onClick = onToggleAllSections
    )
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.common_search),
        onClick = { actions.onSearchToggle(true) }
    )
    NoteListOverflowMenu(
        viewMode = NoteListViewMode.LIST,
        onToggleViewMode = {},
        availableViewModes = emptyList()
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_select_notes)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.SelectAll, contentDescription = null)
            },
            onClick = {
                dismiss()
                actions.onStartNoteSelection()
            }
        )
    }
}
