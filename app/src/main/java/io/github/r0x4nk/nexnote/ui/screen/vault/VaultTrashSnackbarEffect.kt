package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

internal const val VAULT_TRASH_SNACKBAR_MESSAGE = "Moved to Vault trash"
internal const val VAULT_RESTORE_SNACKBAR_MESSAGE = "Restored to Vault"
internal const val VAULT_TRASH_SNACKBAR_UNDO_LABEL = "Undo"

internal sealed interface VaultTrashSnackbarEvent {
    val noteId: Long
    val message: String

    data class MovedToTrash(
        override val noteId: Long
    ) : VaultTrashSnackbarEvent {
        override val message: String = VAULT_TRASH_SNACKBAR_MESSAGE
    }

    data class RestoredFromTrash(
        override val noteId: Long
    ) : VaultTrashSnackbarEvent {
        override val message: String = VAULT_RESTORE_SNACKBAR_MESSAGE
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

    LaunchedEffect(trashEvents, snackbarHostState) {
        trashEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = VAULT_TRASH_SNACKBAR_UNDO_LABEL,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnUndoTrashEvent(event)
            }
        }
    }
}
