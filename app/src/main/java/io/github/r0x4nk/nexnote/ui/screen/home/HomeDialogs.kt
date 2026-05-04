package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template

@Composable
internal fun TemplatePickerDialog(
    templates: List<Template>,
    onSelect: (templateId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a template") },
        text = { TemplatePickerContent(templates, onSelect) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TemplatePickerContent(
    templates: List<Template>,
    onSelect: (templateId: Long) -> Unit
) {
    if (templates.isEmpty()) {
        Text(
            text = "No templates available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    } else {
        Column {
            templates.forEachIndexed { index, template ->
                if (index > 0) TemplateDivider()
                TemplateRow(template = template, onClick = { onSelect(template.id) })
            }
        }
    }
}

@Composable
private fun TemplateDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun TemplateRow(template: Template, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            TemplateIcon()
            TemplateText(template)
        }
    }
}

@Composable
private fun TemplateIcon() {
    Icon(
        imageVector = Icons.Default.Description,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun TemplateText(template: Template) {
    Column(modifier = Modifier.padding(start = 12.dp)) {
        Text(
            text = template.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (template.category.isNotBlank() && template.category != "custom") {
            Text(
                text = template.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
