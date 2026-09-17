package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R

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
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_tag_title, tagName)) },
        text = { DeleteTagDialogText(tagName, noteCount) },
        confirmButton = {
            NexDestructiveButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DeleteTagDialogText(tagName: String, noteCount: Int) {
    Text(
        text = pluralStringResource(
            R.plurals.delete_tag_message, noteCount, tagName, noteCount
        ),
        style = MaterialTheme.typography.bodyMedium
    )
}
