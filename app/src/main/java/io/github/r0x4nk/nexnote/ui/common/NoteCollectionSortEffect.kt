package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Reset only on an explicit sort change, preserving restored scroll on entry. */
@Composable
internal fun NoteCollectionSortEffect(
    sortKey: Any,
    listState: LazyListState,
    gridState: LazyStaggeredGridState? = null,
    firstItemIndex: Int = 0
) {
    var previousSort by remember { mutableStateOf(sortKey) }
    LaunchedEffect(sortKey) {
        if (previousSort != sortKey) {
            previousSort = sortKey
            listState.scrollToItem(firstItemIndex)
            gridState?.scrollToItem(0)
        }
    }
}
