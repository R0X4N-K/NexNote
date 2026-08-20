package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun TagsOverflowMenu(
    expanded: Boolean,
    current: TagSortOrder,
    viewMode: TagsViewMode,
    onSelect: (TagSortOrder) -> Unit,
    onViewModeToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = {
                Text(
                    if (viewMode == TagsViewMode.LIST) "Treemap view" else "List view"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (viewMode == TagsViewMode.LIST) {
                        Icons.AutoMirrored.Filled.ViewQuilt
                    } else {
                        Icons.AutoMirrored.Filled.ViewList
                    },
                    contentDescription = null
                )
            },
            onClick = onViewModeToggle
        )
        HorizontalDivider()
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
