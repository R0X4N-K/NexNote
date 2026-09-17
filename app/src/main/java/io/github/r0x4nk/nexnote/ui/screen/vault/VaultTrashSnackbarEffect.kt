package io.github.r0x4nk.nexnote.ui.screen.vault

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

/**
 * Vault-specific trash snackbar event stream.
 *
 * The snackbar copy is resolved from resources so the effect never carries the
 * decrypted note title or preview. Only the note id travels through the event
 * to wire up the undo action.
 */
internal sealed interface VaultTrashSnackbarEvent {
    val noteId: Long
    val noteIds: List<Long>

    data class MovedToTrash(
        override val noteId: Long,
        val additionalNoteIds: List<Long> = emptyList()
    ) : VaultTrashSnackbarEvent {
        override val noteIds: List<Long>
            get() = listOf(noteId) + additionalNoteIds
    }

    data class RestoredFromTrash(
        override val noteId: Long,
        val additionalNoteIds: List<Long> = emptyList()
    ) : VaultTrashSnackbarEvent {
        override val noteIds: List<Long>
            get() = listOf(noteId) + additionalNoteIds
    }
}

/**
 * Vault-specific counterpart of the Home `TrashSnackbarEffect`.
 *
 * The Home snackbar uses the trashed note title or preview as part of its
 * message (`"Moved \"<label>\" to trash"`). The Vault must not surface any
 * decrypted Vault content through the snackbar — neither the title nor a
 * preview — so this effect renders a fixed, non-sensitive message and only
 * carries the note id internally to wire up the undo action.
 *
 * On `Undo` we call back into the ViewModel with the note id; on dismissal
 * we simply drop the event because Room/Vault flows already reflect the
 * trashed state.
 */
@Composable
internal fun VaultTrashSnackbarEffect(
    trashEvents: Flow<VaultTrashSnackbarEvent>,
    snackbarHostState: SnackbarHostState,
    onUndoTrashEvent: (VaultTrashSnackbarEvent) -> Unit
) {
    val currentOnUndoTrashEvent by rememberUpdatedState(onUndoTrashEvent)
    val resources = LocalResources.current

    LaunchedEffect(trashEvents, snackbarHostState, resources) {
        trashEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.localizedMessage(resources),
                actionLabel = resources.getString(R.string.common_undo),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnUndoTrashEvent(event)
            }
        }
    }
}

private fun VaultTrashSnackbarEvent.localizedMessage(
    resources: android.content.res.Resources
): String {
    val count = noteIds.size
    return when (this) {
        is VaultTrashSnackbarEvent.MovedToTrash ->
            if (count == 1) {
                resources.getString(R.string.vault_trash_snackbar_moved)
            } else {
                resources.getQuantityString(
                    R.plurals.vault_trash_snackbar_moved_many,
                    count,
                    count
                )
            }

        is VaultTrashSnackbarEvent.RestoredFromTrash ->
            if (count == 1) {
                resources.getString(R.string.vault_trash_snackbar_restored)
            } else {
                resources.getQuantityString(
                    R.plurals.vault_trash_snackbar_restored_many,
                    count,
                    count
                )
            }
    }
}
