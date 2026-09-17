package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton
import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.displayLabel

@Composable
internal fun DeleteNoteDialog(
    note: Note?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    note?.let {
        TrashConfirmDialog(
            title = stringResource(R.string.trash_delete_dialog_title),
            text = stringResource(
                R.string.trash_delete_dialog_message,
                it.displayLabel(untitledLabel = stringResource(R.string.untitled_note))
            ),
            confirmText = stringResource(R.string.delete),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun EmptyTrashDialog(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        TrashConfirmDialog(
            title = stringResource(R.string.trash_empty_dialog_title),
            text = stringResource(R.string.trash_empty_dialog_message),
            confirmText = stringResource(R.string.trash_empty_confirm),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun TrashConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        icon = { TrashDeleteIcon() },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TrashErrorTextButton(confirmText, onConfirm) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun TrashDeleteIcon() {
    Icon(
        imageVector        = Icons.Default.DeleteForever,
        contentDescription = null,
        tint               = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun TrashErrorTextButton(
    text: String,
    onClick: () -> Unit
) {
    NexDestructiveButton(onClick = onClick) {
        Text(text)
    }
}
