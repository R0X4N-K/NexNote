package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.MasonryGrid
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState
import io.github.r0x4nk.nexnote.ui.component.NoteCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun LazyListScope.agendaNotesItems(
    notes: List<Note>,
    viewMode: NoteListViewMode,
    noteCardStyle: NoteCardStyle,
    isSearchEmpty: Boolean,
    actions: AgendaActions
) {
    if (notes.isEmpty()) {
        item { AgendaEmptyState(isSearchActive = isSearchEmpty) }
    } else if (viewMode == NoteListViewMode.GRID) {
        item { AgendaNotesGrid(notes, noteCardStyle, actions) }
    } else {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { actions.onNoteClick(note.id) },
                noteCardStyle = noteCardStyle,
                onPin = { actions.onTogglePin(note) },
                onLongPress = { actions.onRequestNoteActions(note) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .animateItem(),
                onTrash = { actions.onRequestTrash(note) }
            )
        }
    }
}

@Composable
internal fun NotesSectionHeader(year: Int, month: Int, day: Int) {
    val label = remember(year, month, day) {
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            .format(cal.time)
            .replaceFirstChar { it.uppercaseChar() }
    }
    Text(
        text = "Notes for $label",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun AgendaNotesGrid(
    notes: List<Note>,
    noteCardStyle: NoteCardStyle,
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
        notes.forEach { note ->
            key(note.id) {
                AgendaGridNoteCard(note, noteCardStyle, actions)
            }
        }
    }
}

@Composable
private fun AgendaGridNoteCard(
    note: Note,
    noteCardStyle: NoteCardStyle,
    actions: AgendaActions
) {
    NoteCard(
        note = note,
        onClick = { actions.onNoteClick(note.id) },
        noteCardStyle = noteCardStyle,
        onPin = { actions.onTogglePin(note) },
        onLongPress = { actions.onRequestNoteActions(note) },
        onTrash = { actions.onRequestTrash(note) }
    )
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
            title = if (isSearchActive) "No results" else "No notes on this day",
            message = if (isSearchActive) "Try different words" else "This date is clear"
        )
    }
}
