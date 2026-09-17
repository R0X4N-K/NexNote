package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.animateNoteItem
import io.github.r0x4nk.nexnote.ui.component.MasonryGrid
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolder
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.noteTagFolderItems

internal fun LazyListScope.agendaNotesItems(
    notes: List<Note>,
    displayItems: List<ScoredNote>,
    viewMode: NoteListViewMode,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    isSearchEmpty: Boolean,
    tagFolders: List<NoteTagFolder>,
    tagFolderExpansionState: NoteTagFolderExpansionState,
    actions: AgendaActions
) {
    if (notes.isEmpty()) {
        item { AgendaEmptyState(isSearchActive = isSearchEmpty) }
    } else {
        when (viewMode) {
            NoteListViewMode.GRID -> {
                item { AgendaNotesGrid(displayItems, noteCardStyle, selectionState, actions) }
            }
            NoteListViewMode.TAGS -> {
                noteTagFolderItems(
                    folders = tagFolders,
                    expansionState = tagFolderExpansionState,
                    horizontalPadding = 16.dp
                ) { scored, modifier ->
                    AgendaNoteCardItem(
                        scored = scored,
                        noteCardStyle = noteCardStyle,
                        selectionState = selectionState,
                        actions = actions,
                        modifier = modifier
                    )
                }
            }
            NoteListViewMode.LIST -> {
                items(displayItems, key = { it.note.id }) { scored ->
                    AgendaNoteCardItem(
                        scored = scored,
                        noteCardStyle = noteCardStyle,
                        selectionState = selectionState,
                        actions = actions,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .then(animateNoteItem())
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaNotesGrid(
    displayItems: List<ScoredNote>,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    actions: AgendaActions
) {
    MasonryGrid(
        columns = 2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
    ) {
        displayItems.forEach { scored ->
            key(scored.note.id) {
                AgendaNoteCardItem(scored, noteCardStyle, selectionState, actions)
            }
        }
    }
}

@Composable
private fun AgendaEmptyState(isSearchActive: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        NexEmptyState(
            icon = if (isSearchActive) Icons.AutoMirrored.Filled.ManageSearch else Icons.Default.EventBusy,
            title = stringResource(
                if (isSearchActive) R.string.agenda_empty_search_title else R.string.agenda_empty_day_title
            ),
            message = stringResource(
                if (isSearchActive) R.string.agenda_empty_search_message else R.string.agenda_empty_day_message
            )
        )
    }
}
