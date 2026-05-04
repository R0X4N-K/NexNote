package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagsTopBar(
    uiState: TagsUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    isSearchActive: Boolean,
    showSortMenu: Boolean,
    actions: TagsActions
) {
    TopAppBar(
        title = { Text("Tags") },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = actions.onSearchOpen) {
                    Icon(Icons.Default.Search, contentDescription = "Search tags")
                }
            }
            Box {
                IconButton(onClick = actions.onSortMenuOpen) {
                    TagsSortIcon(uiState.sortOrder)
                }
                SortDropdownMenu(
                    expanded = showSortMenu,
                    current = uiState.sortOrder,
                    onSelect = actions.onSortSelect,
                    onDismiss = actions.onSortMenuDismiss
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun TagsSortIcon(sortOrder: TagSortOrder) {
    Icon(
        imageVector = Icons.Default.Tag,
        contentDescription = "Sort tags",
        tint = if (sortOrder != TagSortOrder.USAGE_DESC) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        }
    )
}
