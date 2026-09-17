package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorCreationDateDialog(
    uiState: EditorUiState,
    state: EditorScreenState,
    viewModel: EditorViewModel
) {
    if (state.showDatePicker && !uiState.isTemplateMode && !uiState.isReadOnly) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.creationDate
        )
        DatePickerDialog(
            tonalElevation = 1.dp,
            onDismissRequest = { state.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onCreationDateChange(it) }
                    state.showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { state.showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
