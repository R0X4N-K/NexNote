package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TrashSnackbarEffect(
    trashEvents: Flow<TrashedNoteEvent>,
    snackbarHostState: SnackbarHostState,
    onUndoTrash: (Long) -> Unit,
    onConfirmTrash: (Long) -> Unit
) {
    val currentOnUndoTrash by rememberUpdatedState(onUndoTrash)
    val currentOnConfirmTrash by rememberUpdatedState(onConfirmTrash)

    LaunchedEffect(trashEvents, snackbarHostState) {
        trashEvents.collect { event ->
            handleTrashSnackbarEvent(
                event = event,
                showSnackbar = { trashedEvent ->
                    snackbarHostState.showSnackbar(
                        message = trashedEvent.snackbarMessage(),
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                },
                onUndoTrash = currentOnUndoTrash,
                onConfirmTrash = currentOnConfirmTrash
            )
        }
    }
}

internal suspend fun handleTrashSnackbarEvent(
    event: TrashedNoteEvent,
    showSnackbar: suspend (TrashedNoteEvent) -> SnackbarResult,
    onUndoTrash: (Long) -> Unit,
    onConfirmTrash: (Long) -> Unit
) {
    when (showSnackbar(event)) {
        SnackbarResult.ActionPerformed -> onUndoTrash(event.noteId)
        SnackbarResult.Dismissed -> onConfirmTrash(event.noteId)
    }
}
