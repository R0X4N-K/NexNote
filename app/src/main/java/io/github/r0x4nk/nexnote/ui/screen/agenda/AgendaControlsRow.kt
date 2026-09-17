package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
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
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu
import io.github.r0x4nk.nexnote.ui.component.NoteListSortButton
import io.github.r0x4nk.nexnote.ui.component.NoteSearchSortMenu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun AgendaControlsRow(
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    isSearchActive: Boolean,
    searchQuery: String,
    searchSort: NoteSearchSort,
    hasActiveSearchFilters: Boolean,
    searchFocusRequester: FocusRequester,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    noteCount: Int,
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
                searchSort = searchSort,
                hasActiveSearchFilters = hasActiveSearchFilters,
                searchFocusRequester = searchFocusRequester,
                placeholder = stringResource(R.string.agenda_search_day),
                actions = actions
            )
        } else {
            AgendaToolbarControls(
                sortOrder = sortOrder,
                viewMode = viewMode,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                selectedDay = selectedDay,
                noteCount = noteCount,
                actions = actions
            )
        }
    }
}

@Composable
internal fun RowScope.AgendaSearchControls(
    searchQuery: String,
    searchSort: NoteSearchSort,
    hasActiveSearchFilters: Boolean,
    searchFocusRequester: FocusRequester,
    placeholder: String,
    actions: AgendaActions
) {
    NexSearchField(
        value = searchQuery,
        onValueChange = actions.onSearchQueryChange,
        placeholder = placeholder,
        modifier = Modifier
            .weight(1f),
        focusRequester = searchFocusRequester,
        textStyle = MaterialTheme.typography.bodyLarge
    )
    NoteSearchSortMenu(
        selected = searchSort,
        onSelect = actions.onSearchSortChange
    )
    NexIconButton(
        imageVector = Icons.Default.FilterAlt,
        contentDescription = stringResource(R.string.common_filter_search_results),
        selected = hasActiveSearchFilters,
        onClick = actions.onOpenSearchFilters
    )
    NexIconButton(
        imageVector = Icons.Default.Close,
        contentDescription = stringResource(R.string.common_close_search),
        onClick = { actions.onSearchToggle(false) }
    )
}

@Composable
private fun RowScope.AgendaToolbarControls(
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    noteCount: Int,
    actions: AgendaActions
) {
    SelectedDateSummary(
        year = selectedYear,
        month = selectedMonth,
        day = selectedDay,
        noteCount = noteCount,
        modifier = Modifier.weight(1f)
    )
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.common_search),
        onClick = { actions.onSearchToggle(true) }
    )
    NoteListSortButton(
        sortOrder = sortOrder,
        onToggleSortOrder = actions.onToggleSort
    )
    AgendaOverflowMenu(
        viewMode = viewMode,
        actions = actions
    )
}

@Composable
private fun SelectedDateSummary(
    year: Int,
    month: Int,
    day: Int,
    noteCount: Int,
    modifier: Modifier = Modifier
) {
    val noteCountText = pluralStringResource(
        R.plurals.home_note_count,
        noteCount,
        noteCount
    )
    val labels = androidx.compose.runtime.remember(year, month, day, noteCountText) {
        val date = Calendar.getInstance().apply { set(year, month, day) }.time
        val locale = Locale.getDefault()
        AgendaDateLabels(
            date = SimpleDateFormat("d MMMM yyyy", locale).format(date),
            supporting = buildString {
                append(SimpleDateFormat("EEEE", locale).format(date))
                append(" · ")
                append(noteCountText)
            }
        )
    }

    Column(modifier = modifier.padding(start = 4.dp, end = 8.dp)) {
        Text(
            text = labels.date.replaceFirstChar { it.uppercaseChar() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = labels.supporting.replaceFirstChar { it.uppercaseChar() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class AgendaDateLabels(
    val date: String,
    val supporting: String
)

@Composable
private fun AgendaOverflowMenu(
    viewMode: NoteListViewMode,
    actions: AgendaActions
) {
    NoteListOverflowMenu(
        viewMode = viewMode,
        onToggleViewMode = actions.onToggleView
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_select_notes)) },
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
