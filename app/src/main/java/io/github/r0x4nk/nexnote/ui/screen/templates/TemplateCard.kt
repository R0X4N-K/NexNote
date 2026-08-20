package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NoteCollectionCardDefaults
import io.github.r0x4nk.nexnote.ui.component.SelectionIndicator
import io.github.r0x4nk.nexnote.ui.component.SwipeCollectionAction
import io.github.r0x4nk.nexnote.ui.component.SwipeToCollectionActionsContainer
import io.github.r0x4nk.nexnote.ui.component.roundedCombinedClickableTarget
import io.github.r0x4nk.nexnote.util.MarkdownPlainText

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
    if (onDelete == null) {
        TemplateCardSurface(
            template = template,
            onApply = onApply,
            onEdit = onEdit,
            onLongPress = onLongPress,
            selectionMode = selectionMode,
            selected = selected
        )
        return
    }

    SwipeToCollectionActionsContainer(
        endToStartAction = SwipeCollectionAction.Delete("Delete template"),
        onEndToStart = onDelete,
        collapseBeforeEndToStart = false,
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
            NoteCollectionCardDefaults.border(alpha = 0.44f)
        }
    ) {
        TemplateCardBody(
            template = template,
            onEdit = onEdit,
            selectionMode = selectionMode,
            selected = selected
        )
    }
}

@Composable
private fun TemplateCardBody(
    template: Template,
    onEdit: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TemplateCardHeader(template, onEdit, selectionMode, selected)
        Spacer(Modifier.height(12.dp))
        Text(
            text = template.name.ifBlank { "Untitled template" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = rememberTemplatePreview(template),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        TemplateCardFooter(template)
    }
}

@Composable
private fun TemplateCardHeader(
    template: Template,
    onEdit: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TemplateIcon(icon = templateIcon(template.iconName))
        Spacer(Modifier.weight(1f))
        when {
            selectionMode -> SelectionIndicator(selected = selected)
            onEdit != null -> NexIconButton(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit ${template.name}",
                onClick = onEdit
            )
        }
    }
}

@Composable
private fun TemplateIcon(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun TemplateCardFooter(template: Template) {
    Column {
        val category = template.category
            .takeUnless { it.isBlank() || it == "custom" }
            ?.replaceFirstChar { it.uppercaseChar() }

        if (category != null || template.isMarkdown) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (category != null) {
                    TemplateBadge(category)
                    Spacer(Modifier.width(6.dp))
                }
                if (template.isMarkdown) {
                    TemplateBadge("MD")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Use template",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun TemplateBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun rememberTemplatePreview(template: Template): String =
    remember(template.content, template.isMarkdown) {
        val text = if (template.isMarkdown) {
            MarkdownPlainText.fromMarkdown(template.content)
        } else {
            template.content
        }
        text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(" · ")
            .take(180)
            .ifBlank { "A clean page, ready for your ideas." }
    }

/** Resolves persisted icon names without leaking storage strings into the UI layout. */
private fun templateIcon(iconName: String): ImageVector = when (iconName) {
    "shopping_cart" -> Icons.Default.ShoppingCart
    "work" -> Icons.Default.Work
    "check_box" -> Icons.Default.CheckBox
    "book" -> Icons.AutoMirrored.Filled.MenuBook
    "groups" -> Icons.Default.Groups
    else -> Icons.Default.Description
}
