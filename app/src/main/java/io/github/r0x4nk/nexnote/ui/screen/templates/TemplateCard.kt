package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template

@Composable
internal fun TemplateCard(
    template: Template,
    onApply: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        onClick = onApply,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemplateCardText(template = template, modifier = Modifier.weight(1f))
            TemplateCardActions(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun TemplateCardText(
    template: Template,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = template.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (template.content.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = template.content.take(80),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (template.isMarkdown) {
            Text(
                text = "MD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TemplateCardActions(
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    if (onEdit != null) {
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
    if (onDelete != null) {
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}
