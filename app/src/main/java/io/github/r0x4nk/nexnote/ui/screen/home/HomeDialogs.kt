package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState

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
    var query by rememberSaveable { mutableStateOf("") }
    val filteredTemplates = remember(templates, query) {
        templates.filter { template -> template.matchesQuery(query) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (templates.isNotEmpty()) {
            TemplateSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        TemplatePickerResults(
            templates = templates,
            filteredTemplates = filteredTemplates,
            onSelect = onSelect
        )
    }
}

@Composable
private fun TemplateSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyLarge,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear template search"
                    )
                }
            }
        },
        placeholder = { Text("Search templates") }
    )
}

@Composable
private fun TemplatePickerResults(
    templates: List<Template>,
    filteredTemplates: List<Template>,
    onSelect: (templateId: Long) -> Unit
) {
    when {
        templates.isEmpty() -> TemplatePickerEmptyState(
            title = "No templates available",
            message = "Create a template first"
        )
        filteredTemplates.isEmpty() -> TemplatePickerEmptyState(
            title = "No matching templates",
            message = "Try a different name, category, or phrase"
        )
        else -> LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = filteredTemplates,
                key = { template -> template.id }
            ) { template ->
                TemplateRow(
                    template = template,
                    onClick = { onSelect(template.id) }
                )
            }
        }
    }
}

@Composable
private fun TemplatePickerEmptyState(
    title: String,
    message: String
) {
    NexEmptyState(
        icon = Icons.AutoMirrored.Filled.ManageSearch,
        title = title,
        message = message,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TemplateRow(template: Template, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            TemplateIcon()
            Spacer(Modifier.width(12.dp))
            TemplateText(template, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TemplateIcon() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(9.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TemplateText(
    template: Template,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = template.name.ifBlank { "Untitled template" },
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (template.content.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = template.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        TemplateMetadataRow(template)
    }
}

@Composable
private fun TemplateMetadataRow(template: Template) {
    val category = template.category.takeIf { it.isNotBlank() && it != "custom" }
    if (category == null && !template.isMarkdown) return

    Row(
        modifier = Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (category != null) {
            TemplateBadge(category)
        }
        if (template.isMarkdown) {
            TemplateBadge("MD")
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Template.matchesQuery(query: String): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return true

    return name.contains(normalizedQuery, ignoreCase = true) ||
        category.contains(normalizedQuery, ignoreCase = true) ||
        content.contains(normalizedQuery, ignoreCase = true)
}
