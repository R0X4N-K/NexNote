package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.common.SortOrder

/** Toggles the chronological order of a note or template collection. */
@Composable
internal fun NoteListSortButton(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.SwapVert,
        contentDescription = stringResource(
            if (sortOrder == SortOrder.MODIFIED_DESC) {
                R.string.common_sort_oldest_first
            } else {
                R.string.common_sort_newest_first
            }
        ),
        onClick = onToggleSortOrder
    )
}
