package io.github.r0x4nk.nexnote.ui.common

import android.content.res.Resources
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import io.github.r0x4nk.nexnote.R
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TrashSnackbarEffect(
    trashEvents: Flow<TrashedNoteEvent>,
    snackbarHostState: SnackbarHostState,
    onUndoTrash: (Collection<Long>) -> Unit,
    onConfirmTrash: () -> Unit
) {
    val currentOnUndoTrash by rememberUpdatedState(onUndoTrash)
    val currentOnConfirmTrash by rememberUpdatedState(onConfirmTrash)
    val resources = LocalResources.current

    LaunchedEffect(trashEvents, snackbarHostState, resources) {
        trashEvents.collect { event ->
            handleTrashSnackbarEvent(
                event = event,
                showSnackbar = { trashedEvent ->
                    snackbarHostState.showSnackbar(
                        message = trashedEvent.snackbarMessage(resources),
                        actionLabel = resources.getString(R.string.common_undo),
                        duration = SnackbarDuration.Long
                    )
                },
                onUndoTrash = currentOnUndoTrash,
                onConfirmTrash = currentOnConfirmTrash
            )
        }
    }
}

internal fun TrashedNoteEvent.snackbarMessage(resources: Resources): String =
    if (noteIds.size == 1) {
        resources.getString(R.string.trash_snackbar_moved_single, noteLabel)
    } else {
        resources.getQuantityString(
            R.plurals.trash_snackbar_moved_many,
            noteIds.size,
            noteIds.size
        )
    }

internal suspend fun handleTrashSnackbarEvent(
    event: TrashedNoteEvent,
    showSnackbar: suspend (TrashedNoteEvent) -> SnackbarResult,
    onUndoTrash: (Collection<Long>) -> Unit,
    onConfirmTrash: () -> Unit
) {
    when (showSnackbar(event)) {
        SnackbarResult.ActionPerformed -> onUndoTrash(event.noteIds)
        SnackbarResult.Dismissed -> onConfirmTrash()
    }
}
