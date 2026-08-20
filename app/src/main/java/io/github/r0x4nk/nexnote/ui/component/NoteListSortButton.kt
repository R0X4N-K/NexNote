package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.ui.common.SortOrder

/** Toggles the chronological order of a note or template collection. */
@Composable
internal fun NoteListSortButton(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.SwapVert,
        contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC) {
            "Sort oldest first"
        } else {
            "Sort newest first"
        },
        onClick = onToggleSortOrder
    )
}
