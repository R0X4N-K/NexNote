package com.example.nexnote.ui.screen.tags

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun TagsDialogHost(
    activeDialog: TagsDialog,
    actions: TagsActions
) {
    val dialog = activeDialog
    if (dialog is TagsDialog.ConfirmDelete) {
        DeleteTagDialog(
            tagName = dialog.tag.name,
            noteCount = dialog.tag.noteCount,
            onConfirm = { actions.onConfirmDelete(dialog.tag) },
            onDismiss = actions.onDismissDialog
        )
    }
}

@Composable
private fun DeleteTagDialog(
    tagName: String,
    noteCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete #$tagName?") },
        text = { DeleteTagDialogText(tagName, noteCount) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteTagDialogText(tagName: String, noteCount: Int) {
    Text(
        text = buildString {
            append("The tag #$tagName will be removed from ")
            append(if (noteCount == 1) "1 note" else "$noteCount notes")
            append(". The '#' prefix will be stripped from each occurrence, ")
            append("but the word \"$tagName\" will remain in the note text.")
        },
        style = MaterialTheme.typography.bodyMedium
    )
}
