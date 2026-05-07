package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Sell
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState

@Composable
internal fun EmptyState(
    isSearchActive: Boolean,
    hasTagFilter: Boolean,
    modifier: Modifier = Modifier
) {
    NexEmptyState(
        icon = emptyStateIcon(isSearchActive, hasTagFilter),
        title = emptyStateTitle(isSearchActive, hasTagFilter),
        message = emptyStateBody(isSearchActive, hasTagFilter),
        modifier = modifier
    )
}

private fun emptyStateIcon(isSearchActive: Boolean, hasTagFilter: Boolean): ImageVector =
    when {
        isSearchActive -> Icons.AutoMirrored.Filled.ManageSearch
        hasTagFilter -> Icons.Default.Sell
        else -> Icons.AutoMirrored.Filled.Note
    }

private fun emptyStateTitle(isSearchActive: Boolean, hasTagFilter: Boolean): String =
    when {
        isSearchActive -> "No results"
        hasTagFilter -> "No notes with these tags"
        else -> "No notes"
    }

private fun emptyStateBody(isSearchActive: Boolean, hasTagFilter: Boolean): String =
    when {
        isSearchActive -> "Try different words"
        hasTagFilter -> "Try removing some tag filters"
        else -> "Use the + button below to create your first note"
    }
