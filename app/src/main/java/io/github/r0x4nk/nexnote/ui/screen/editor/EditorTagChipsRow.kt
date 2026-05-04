package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.component.TagChip

@Composable
internal fun TagChipsEditorRow(
    tags: List<Tag>,
    selectedTag: String?,
    onTagClick: (String) -> Unit,
    onClearSelection: () -> Unit,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipScrollState = rememberScrollState()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(chipScrollState)
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tags.forEach { tag ->
                TagChip(
                    tagName = tag.name,
                    onClick = { onTagClick(tag.name) },
                    isSelected = tag.name == selectedTag
                )
            }
            if (selectedTag != null) {
                TextButton(onClick = onClearSelection) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = if (isPinned) {
                    "Unpin tag row (enable auto-hide)"
                } else {
                    "Pin tag row (disable auto-hide)"
                },
                tint = if (isPinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
