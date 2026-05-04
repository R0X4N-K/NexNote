package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            title = "Delete permanently",
            text = deleteNoteDialogText(it),
            confirmText = "Delete",
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
            title = "Empty trash",
            text = "Permanently delete all notes in the trash? This cannot be undone.",
            confirmText = "Empty",
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
        onDismissRequest = onDismiss,
        icon = { TrashDeleteIcon() },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TrashErrorTextButton(confirmText, onConfirm) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
    TextButton(onClick = onClick) {
        Text(text, color = MaterialTheme.colorScheme.error)
    }
}

private fun deleteNoteDialogText(note: Note): String {
    val label = note.displayLabel()
    return "Permanently delete \"$label\"? This cannot be undone."
}
