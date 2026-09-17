package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note

/**
 * List-level creation-date editor shared by Home, Agenda, Tags and Vault.
 *
 * It edits only the note's user-visible creation date; persistence stays with
 * the calling ViewModel so this component remains a pure dialog. The Vault
 * route reuses the same UI without ever reading the note body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteCreationDatePickerDialog(
    note: Note?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (note == null) return

    val pickerState = rememberDatePickerState(initialSelectedDateMillis = note.creationDate)
    DatePickerDialog(
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let(onConfirm)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}
