package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState

@Composable
internal fun TagsEmptyState(hasSearch: Boolean, modifier: Modifier = Modifier) {
    NexEmptyState(
        icon = if (hasSearch) Icons.AutoMirrored.Filled.ManageSearch else Icons.Default.Tag,
        title = if (hasSearch) "No tags found" else "No tags yet",
        message = if (hasSearch) {
            "Try a different search"
        } else {
            "Write #tagName in any note to create a tag"
        },
        modifier = modifier
    )
}
