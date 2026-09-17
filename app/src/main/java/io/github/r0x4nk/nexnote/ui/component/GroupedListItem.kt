package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class GroupedItemPosition { FIRST, MIDDLE, LAST, ONLY }

/** A visual container only; children retain their own actions and semantics. */
@Composable
internal fun GroupedListItem(
    position: GroupedItemPosition,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val outerTop = position == GroupedItemPosition.FIRST || position == GroupedItemPosition.ONLY
    val outerBottom = position == GroupedItemPosition.LAST || position == GroupedItemPosition.ONLY
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = if (outerTop) 18.dp else 6.dp,
            topEnd = if (outerTop) 18.dp else 6.dp,
            bottomStart = if (outerBottom) 18.dp else 6.dp,
            bottomEnd = if (outerBottom) 18.dp else 6.dp
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
