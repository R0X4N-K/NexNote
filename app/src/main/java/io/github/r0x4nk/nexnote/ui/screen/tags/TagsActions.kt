package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag

internal data class TagsActions(
    val onSearchOpen: () -> Unit,
    val onSearchClose: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onSortMenuOpen: () -> Unit,
    val onSortMenuDismiss: () -> Unit,
    val onSortSelect: (TagSortOrder) -> Unit,
    val onViewModeToggle: () -> Unit,
    val onTagClick: (String) -> Unit,
    val onNoteClick: (Long) -> Unit,
    val onRequestNoteActions: (Note) -> Unit,
    val onDeleteClick: (Tag) -> Unit,
    val onConfirmDelete: (Tag) -> Unit,
    val onDismissDialog: () -> Unit
)

@Composable
internal fun rememberTagsActions(
    viewModel: TagsViewModel,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSortMenuChange: (Boolean) -> Unit
): TagsActions {
    return remember(
        viewModel,
        onNoteClick,
        onRequestNoteActions,
        onSearchActiveChange,
        onSortMenuChange
    ) {
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
            onViewModeToggle = {
                viewModel.toggleViewMode()
                onSortMenuChange(false)
            },
            onTagClick = viewModel::toggleTagSelection,
            onNoteClick = onNoteClick,
            onRequestNoteActions = onRequestNoteActions,
            onDeleteClick = viewModel::requestDeleteTag,
            onConfirmDelete = viewModel::confirmDeleteTag,
            onDismissDialog = viewModel::dismissDialog
        )
    }
}
