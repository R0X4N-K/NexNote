package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton
import androidx.compose.runtime.Composable

@Composable
internal fun TemplatesDeleteDialog(
    dialog: TemplatesDialog,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    when (dialog) {
        is TemplatesDialog.ConfirmDelete -> {
            AlertDialog(
                tonalElevation = 1.dp,
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.templates_delete_title)) },
                text = {
                    Text(stringResource(R.string.templates_delete_message, dialog.template.name))
                },
                confirmButton = {
                    NexDestructiveButton(onClick = onConfirmDelete) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        is TemplatesDialog.ConfirmDeleteSelection -> {
            AlertDialog(
                tonalElevation = 1.dp,
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.templates_delete_selection_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.templates_delete_selection_message,
                            dialog.templates.size
                        )
                    )
                },
                confirmButton = {
                    NexDestructiveButton(onClick = onConfirmDelete) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        TemplatesDialog.None -> Unit
    }
}
