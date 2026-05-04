package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

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
        title = {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = {
            if (!isSearchActive) {
                NexIconButton(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search tags",
                    onClick = actions.onSearchOpen
                )
            }
            Box {
                TagsSortButton(
                    sortOrder = uiState.sortOrder,
                    onClick = actions.onSortMenuOpen
                )
                SortDropdownMenu(
                    expanded = showSortMenu,
                    current = uiState.sortOrder,
                    onSelect = actions.onSortSelect,
                    onDismiss = actions.onSortMenuDismiss
                )
            }
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun TagsSortButton(sortOrder: TagSortOrder, onClick: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.Tag,
        contentDescription = "Sort tags",
        onClick = onClick,
        selected = sortOrder != TagSortOrder.USAGE_DESC
    )
}
