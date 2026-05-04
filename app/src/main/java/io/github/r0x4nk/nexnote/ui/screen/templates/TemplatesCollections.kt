package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.NexSectionLabel

@Composable
internal fun TemplatesCollection(
    uiState: TemplatesUiState,
    padding: PaddingValues,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit
) {
    val contentModifier = Modifier.fillMaxSize().padding(padding)

    if (uiState.viewMode == NoteListViewMode.GRID) {
        TemplatesGrid(
            predefined = uiState.predefined,
            custom = uiState.custom,
            onApply = onApply,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = contentModifier
        )
    } else {
        TemplatesList(
            predefined = uiState.predefined,
            custom = uiState.custom,
            onApply = onApply,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = contentModifier
        )
    }
}

@Composable
private fun TemplatesList(
    predefined: List<Template>,
    custom: List<Template>,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (predefined.isNotEmpty()) {
            item { SectionHeader("Predefined") }
            items(predefined, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = { onApply(template.id) },
                    onEdit = null,
                    onDelete = null
                )
            }
        }

        if (custom.isNotEmpty()) {
            item { SectionHeader("My templates") }
            items(custom, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = { onApply(template.id) },
                    onEdit = { onEdit(template.id) },
                    onDelete = { onDelete(template) }
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TemplatesGrid(
    predefined: List<Template>,
    custom: List<Template>,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (predefined.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Predefined")
            }
            items(predefined, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = { onApply(template.id) },
                    onEdit = null,
                    onDelete = null
                )
            }
        }

        if (custom.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("My templates")
            }
            items(custom, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = { onApply(template.id) },
                    onEdit = { onEdit(template.id) },
                    onDelete = { onDelete(template) }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    NexSectionLabel(
        text = title,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
