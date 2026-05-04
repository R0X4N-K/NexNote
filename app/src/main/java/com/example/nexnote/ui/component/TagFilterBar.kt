package com.example.nexnote.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A horizontally scrollable bar showing the currently active tag filters.
 *
 * Role: UI component layer — stateless, reused on the Home and Agenda screens.
 *
 * Each active filter is displayed as a dismissible [TagChip]. The bar is hidden
 * entirely when [selectedTags] is empty, so callers do not need to check before
 * composing this function.
 *
 * Layout:
 *   "Filter:" label → dismissible chip per tag → "Clear all" button
 *
 * @param selectedTags Set of active tag filter names (lowercase, without '#').
 * @param onTagRemove  Called with the tag name when its chip's '×' is tapped.
 * @param onClearAll   Called when the "Clear all" button is tapped.
 */
@Composable
fun TagFilterBar(
    selectedTags: Set<String>,
    onTagRemove: (tagName: String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTags.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text  = "Filter:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // Sort for stable ordering during multi-selection.
        selectedTags.sorted().forEach { tagName ->
            TagChip(
                tagName    = tagName,
                onClick    = { onTagRemove(tagName) },
                isSelected = true,
                dismissible = true,
                onDismiss  = { onTagRemove(tagName) }
            )
        }

        TextButton(onClick = onClearAll) {
            Text(
                text  = "Clear all",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
