package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Sell
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R

@Composable
internal fun EmptyState(
    isSearchActive: Boolean,
    hasTagFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyNotesIllustration(icon = emptyStateIcon(isSearchActive, hasTagFilter))
        Spacer(Modifier.size(24.dp))
        Text(
            text = emptyStateTitle(isSearchActive, hasTagFilter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = emptyStateBody(isSearchActive, hasTagFilter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun emptyStateIcon(isSearchActive: Boolean, hasTagFilter: Boolean): ImageVector =
    when {
        isSearchActive -> Icons.AutoMirrored.Filled.ManageSearch
        hasTagFilter -> Icons.Default.Sell
        else -> Icons.AutoMirrored.Filled.Note
    }

@Composable
private fun emptyStateTitle(isSearchActive: Boolean, hasTagFilter: Boolean): String =
    when {
        isSearchActive -> stringResource(R.string.home_empty_title_no_results)
        hasTagFilter -> stringResource(R.string.home_empty_title_tags)
        else -> stringResource(R.string.no_notes)
    }

@Composable
private fun emptyStateBody(isSearchActive: Boolean, hasTagFilter: Boolean): String =
    when {
        isSearchActive -> stringResource(R.string.home_empty_body_no_results)
        hasTagFilter -> stringResource(R.string.home_empty_body_tags)
        else -> stringResource(R.string.home_empty_body_no_notes)
    }
