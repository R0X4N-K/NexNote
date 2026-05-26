package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NoteCollectionCardDefaults
import io.github.r0x4nk.nexnote.ui.component.SelectionIndicator
import io.github.r0x4nk.nexnote.ui.component.SwipeToDeleteContainer
import io.github.r0x4nk.nexnote.ui.component.roundedCombinedClickableTarget

@Composable
internal fun TemplateCard(
    template: Template,
    onApply: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onLongPress: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    if (onDelete != null) {
        SwipeToDeleteContainer(
            onDelete = onDelete,
            contentDescription = "Delete template",
            collapseBeforeDelete = false,
            enabled = !selectionMode
        ) {
            TemplateCardSurface(
                template = template,
                onApply = onApply,
                onEdit = onEdit,
                onLongPress = onLongPress,
                selectionMode = selectionMode,
                selected = selected
            )
        }
    } else {
        TemplateCardSurface(
            template = template,
            onApply = onApply,
            onEdit = onEdit,
            onLongPress = onLongPress,
            selectionMode = selectionMode,
            selected = selected
        )
    }
}

@Composable
private fun TemplateCardSurface(
    template: Template,
    onApply: () -> Unit,
    onEdit: (() -> Unit)?,
    onLongPress: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean
) {
    val shape = NoteCollectionCardDefaults.shape
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .roundedCombinedClickableTarget(
                shape = shape,
                onClick = onApply,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                NoteCollectionCardDefaults.containerColor()
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = NoteCollectionCardDefaults.defaultElevation
        ),
        shape = shape,
        border = if (selected) {
            NoteCollectionCardDefaults.border(
                alpha = 1f,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            NoteCollectionCardDefaults.border()
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemplateCardText(template = template, modifier = Modifier.weight(1f))
            if (selectionMode) {
                SelectionIndicator(selected = selected)
            } else {
                TemplateCardActions(onEdit = onEdit)
            }
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
private fun TemplateCardActions(onEdit: (() -> Unit)?) {
    if (onEdit != null) {
        NexIconButton(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit",
            onClick = onEdit
        )
    }
}
