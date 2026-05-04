package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState

/**
 * Trash screen body. Renders one of three states based on [uiState]:
 *  - loading: centered progress indicator;
 *  - empty: [TrashEmptyState] using the shared [NexEmptyState] component;
 *  - populated: [TrashNotesList] with restore / delete actions per note.
 *
 * The composable is stateless: parent owns the data and dispatches the
 * [onRestore] / [onDeletePermanently] callbacks per item.
 */
@Composable
internal fun TrashContent(
    uiState: TrashUiState,
    onRestore: (Note) -> Unit,
    onDeletePermanently: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.notes.isEmpty() -> TrashEmptyState()
            else -> TrashNotesList(
                notes = uiState.notes,
                onRestore = onRestore,
                onDeletePermanently = onDeletePermanently
            )
        }
    }
}

@Composable
private fun TrashEmptyState() {
    NexEmptyState(
        icon = Icons.Default.Delete,
        title = "Trash is empty",
        message = "Deleted notes will appear here",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TrashNotesList(
    notes: List<Note>,
    onRestore: (Note) -> Unit,
    onDeletePermanently: (Note) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = notes,
            key = { note -> note.id },
            contentType = { "trash_note_card" }
        ) { note ->
            TrashNoteCard(
                note = note,
                onRestore = { onRestore(note) },
                onDeletePermanently = { onDeletePermanently(note) }
            )
        }
    }
}
