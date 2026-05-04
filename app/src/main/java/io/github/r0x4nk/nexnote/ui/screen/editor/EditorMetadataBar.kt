package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.DateUtils

@Composable
internal fun MetadataBar(
    charCount: Int,
    lastModifiedDate: Long?,
    creationDate: Long,
    onCreationDateTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val charCountColor = when {
        charCount >= 400_000 -> MaterialTheme.colorScheme.error
        charCount >= 50_000 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$charCount characters",
                style = MaterialTheme.typography.labelSmall,
                color = charCountColor
            )
            if (lastModifiedDate != null) {
                Text(
                    text = "  ·  Edited: ${DateUtils.formatRelative(lastModifiedDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
        Text(
            text = "Created: ${DateUtils.formatDate(creationDate)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            modifier = Modifier
                .clickable(onClick = onCreationDateTap)
                .padding(vertical = 2.dp)
        )
    }
}
