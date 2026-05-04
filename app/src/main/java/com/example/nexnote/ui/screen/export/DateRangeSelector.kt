package com.example.nexnote.ui.screen.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DateRangeSelector(
    dateFrom: Long?,
    dateTo: Long?,
    onRangeSelected: (Long, Long) -> Unit
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    DateRangeButtons(
        fromText = dateFrom?.let { fmt.format(Date(it)) } ?: "From",
        toText = dateTo?.let { fmt.format(Date(it)) } ?: "To",
        onFromClick = { showFromPicker = true },
        onToClick = { showToPicker = true }
    )
    DateRangeDialogs(
        showFromPicker = showFromPicker,
        showToPicker = showToPicker,
        dateFrom = dateFrom,
        dateTo = dateTo,
        onDismissFrom = { showFromPicker = false },
        onDismissTo = { showToPicker = false },
        onRangeSelected = onRangeSelected
    )
}

@Composable
private fun DateRangeButtons(
    fromText: String,
    toText: String,
    onFromClick: () -> Unit,
    onToClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRangeButton(
            text = fromText,
            onClick = onFromClick,
            modifier = Modifier.weight(1f)
        )
        DateRangeButton(
            text = toText,
            onClick = onToClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateRangeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .padding(end = 4.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DateRangeDialogs(
    showFromPicker: Boolean,
    showToPicker: Boolean,
    dateFrom: Long?,
    dateTo: Long?,
    onDismissFrom: () -> Unit,
    onDismissTo: () -> Unit,
    onRangeSelected: (Long, Long) -> Unit
) {
    if (showFromPicker) {
        DateFromPickerDialog(dateFrom, dateTo, onDismissFrom, onRangeSelected)
    }
    if (showToPicker) {
        DateToPickerDialog(dateFrom, dateTo, onDismissTo, onRangeSelected)
    }
}

@Composable
private fun DateFromPickerDialog(
    dateFrom: Long?,
    dateTo: Long?,
    onDismiss: () -> Unit,
    onRangeSelected: (Long, Long) -> Unit
) {
    DateRangePickerDialog(
        initialSelectedDateMillis = dateFrom,
        onDismiss = onDismiss,
        onConfirm = { selected ->
            val to = dateTo ?: selected
            onRangeSelected(selected, maxOf(selected, to))
            onDismiss()
        }
    )
}

@Composable
private fun DateToPickerDialog(
    dateFrom: Long?,
    dateTo: Long?,
    onDismiss: () -> Unit,
    onRangeSelected: (Long, Long) -> Unit
) {
    DateRangePickerDialog(
        initialSelectedDateMillis = dateTo,
        onDismiss = onDismiss,
        onConfirm = { selected ->
            val from = dateFrom ?: selected
            onRangeSelected(minOf(from, selected), selected)
            onDismiss()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialSelectedDateMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = pickerState.selectedDateMillis ?: return@TextButton
                onConfirm(selected)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}
