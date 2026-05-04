package com.example.nexnote.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EmptyState(
    isSearchActive: Boolean,
    hasTagFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emptyStateTitle(isSearchActive, hasTagFilter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = emptyStateBody(isSearchActive, hasTagFilter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
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
