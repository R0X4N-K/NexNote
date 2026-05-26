package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                onDismissRequest = onDismiss,
                title = { Text("Delete template") },
                text = { Text("Delete \"${dialog.template.name}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is TemplatesDialog.ConfirmDeleteSelection -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Delete templates") },
                text = {
                    Text(
                        "Delete ${dialog.templates.size} selected templates? " +
                            "This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        TemplatesDialog.None -> Unit
    }
}
