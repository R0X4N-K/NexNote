package com.example.nexnote.ui.screen.tags

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun SortDropdownMenu(
    expanded: Boolean,
    current: TagSortOrder,
    onSelect: (TagSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortOption(TagSortOrder.USAGE_DESC, "Usage: most first", current, onSelect)
        SortOption(TagSortOrder.USAGE_ASC, "Usage: least first", current, onSelect)
        SortOption(TagSortOrder.DATE_DESC, "Date: newest first", current, onSelect)
        SortOption(TagSortOrder.DATE_ASC, "Date: oldest first", current, onSelect)
    }
}

@Composable
private fun SortOption(
    order: TagSortOrder,
    label: String,
    current: TagSortOrder,
    onSelect: (TagSortOrder) -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (order == current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = { onSelect(order) }
    )
}
