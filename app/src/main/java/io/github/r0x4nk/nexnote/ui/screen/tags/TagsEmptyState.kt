package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState

@Composable
internal fun TagsEmptyState(hasSearch: Boolean, modifier: Modifier = Modifier) {
    NexEmptyState(
        icon = if (hasSearch) Icons.AutoMirrored.Filled.ManageSearch else Icons.Default.Tag,
        title = if (hasSearch) stringResource(R.string.no_tags_found) else stringResource(R.string.no_tags_yet),
        message = if (hasSearch) {
            stringResource(R.string.try_different_search)
        } else {
            stringResource(R.string.create_tag_hint)
        },
        modifier = modifier
    )
}
