package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object NoteCollectionLayoutDefaults {
    private val topPadding = 8.dp
    private val listHorizontalPadding = 16.dp
    private val gridHorizontalPadding = 12.dp

    val itemSpacing: Dp = 8.dp
    val defaultBottomPadding: Dp = 8.dp

    fun listContentPadding(
        bottomPadding: Dp = defaultBottomPadding
    ): PaddingValues = PaddingValues(
        start = listHorizontalPadding,
        top = topPadding,
        end = listHorizontalPadding,
        bottom = bottomPadding
    )

    fun gridContentPadding(
        bottomPadding: Dp = defaultBottomPadding
    ): PaddingValues = PaddingValues(
        start = gridHorizontalPadding,
        top = topPadding,
        end = gridHorizontalPadding,
        bottom = bottomPadding
    )
}
