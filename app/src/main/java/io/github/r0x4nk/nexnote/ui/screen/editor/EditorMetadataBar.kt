package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.DateUtils

/**
 * Compact, single-line metadata strip designed to sit inside the editor toolbar area.
 * Shows character count, last-edited date, and a tappable creation date — all in one row
 * to avoid consuming vertical space below the content field.
 */
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$charCount chars",
            style = MaterialTheme.typography.labelSmall,
            color = charCountColor,
            maxLines = 1
        )
        if (lastModifiedDate != null) {
            Text(
                text = " · ${DateUtils.formatRelative(lastModifiedDate)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = " · ${DateUtils.formatDate(creationDate)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(onClick = onCreationDateTap)
                .padding(vertical = 2.dp)
        )
    }
}
