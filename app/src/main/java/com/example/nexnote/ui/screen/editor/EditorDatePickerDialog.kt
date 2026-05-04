package com.example.nexnote.ui.screen.editor

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorCreationDateDialog(
    uiState: EditorUiState,
    state: EditorScreenState,
    viewModel: EditorViewModel
) {
    if (state.showDatePicker && !uiState.isTemplateMode) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.creationDate
        )
        DatePickerDialog(
            onDismissRequest = { state.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onCreationDateChange(it) }
                    state.showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { state.showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
