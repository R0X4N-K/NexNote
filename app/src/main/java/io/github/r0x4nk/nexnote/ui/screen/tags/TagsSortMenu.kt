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
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R

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
                    if (viewMode == TagsViewMode.LIST) stringResource(R.string.treemap_view) else stringResource(R.string.list_view)
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
        SortOption(TagSortOrder.USAGE_DESC, stringResource(R.string.sort_usage_desc), current, onSelect)
        SortOption(TagSortOrder.USAGE_ASC, stringResource(R.string.sort_usage_asc), current, onSelect)
        SortOption(TagSortOrder.DATE_DESC, stringResource(R.string.sort_date_desc), current, onSelect)
        SortOption(TagSortOrder.DATE_ASC, stringResource(R.string.sort_date_asc), current, onSelect)
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
