package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimezoneDropdown(
    selectedId: String,
    availableTimezones: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember(selectedId) {
        mutableStateOf(if (selectedId.isEmpty()) "" else selectedId)
    }
    val filtered = rememberFilteredTimezones(searchQuery, availableTimezones)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TimezoneSearchField(
            selectedId = selectedId,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it; expanded = true }
        )
        TimezoneOptionsMenu(
            expanded = expanded && filtered.isNotEmpty(),
            filteredTimezones = filtered,
            onDismiss = { expanded = false },
            onSelect = { timezone ->
                onSelect(timezone)
                searchQuery = timezone
                expanded = false
            }
        )
    }
}

@Composable
private fun rememberFilteredTimezones(
    searchQuery: String,
    availableTimezones: List<String>
): List<String> {
    return remember(searchQuery, availableTimezones) {
        if (searchQuery.isBlank()) availableTimezones.take(50)
        else availableTimezones.filter { it.contains(searchQuery, ignoreCase = true) }.take(50)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.TimezoneSearchField(
    selectedId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = {
            Text(if (selectedId.isEmpty()) "Timezone (device default)" else "Timezone")
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.TimezoneOptionsMenu(
    expanded: Boolean,
    filteredTimezones: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Auto (device default)") },
            onClick = { onSelect("") }
        )
        HorizontalDivider()
        filteredTimezones.forEach { timezone ->
            DropdownMenuItem(
                text = { Text(timezone) },
                onClick = { onSelect(timezone) }
            )
        }
    }
}
