package com.example.nexnote.ui.screen.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.nexnote.domain.model.Tag

internal data class TagsActions(
    val onSearchOpen: () -> Unit,
    val onSearchClose: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onSortMenuOpen: () -> Unit,
    val onSortMenuDismiss: () -> Unit,
    val onSortSelect: (TagSortOrder) -> Unit,
    val onTagClick: (String) -> Unit,
    val onNoteClick: (Long) -> Unit,
    val onDeleteClick: (Tag) -> Unit,
    val onConfirmDelete: (Tag) -> Unit,
    val onDismissDialog: () -> Unit
)

@Composable
internal fun rememberTagsActions(
    viewModel: TagsViewModel,
    onNoteClick: (Long) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSortMenuChange: (Boolean) -> Unit
): TagsActions {
    return remember(viewModel, onNoteClick, onSearchActiveChange, onSortMenuChange) {
        TagsActions(
            onSearchOpen = { onSearchActiveChange(true) },
            onSearchClose = {
                viewModel.clearSearch()
                onSearchActiveChange(false)
            },
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSortMenuOpen = { onSortMenuChange(true) },
            onSortMenuDismiss = { onSortMenuChange(false) },
            onSortSelect = { order ->
                viewModel.setSortOrder(order)
                onSortMenuChange(false)
            },
            onTagClick = viewModel::toggleTagSelection,
            onNoteClick = onNoteClick,
            onDeleteClick = viewModel::requestDeleteTag,
            onConfirmDelete = viewModel::confirmDeleteTag,
            onDismissDialog = viewModel::dismissDialog
        )
    }
}
