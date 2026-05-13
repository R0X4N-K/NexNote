package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.NexSectionLabel
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults

@Composable
internal fun TemplatesCollection(
    uiState: TemplatesUiState,
    padding: PaddingValues,
    floatingBottomPadding: Dp,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit
) {
    val contentModifier = Modifier.fillMaxSize().padding(padding)
    val bottomContentPadding = RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)

    if (uiState.viewMode == NoteListViewMode.GRID) {
        TemplatesGrid(
            predefined = uiState.predefined,
            custom = uiState.custom,
            bottomContentPadding = bottomContentPadding,
            onApply = onApply,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = contentModifier
        )
    } else {
        TemplatesList(
            predefined = uiState.predefined,
            custom = uiState.custom,
            bottomContentPadding = bottomContentPadding,
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
    bottomContentPadding: Dp,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = bottomContentPadding
        ),
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
    }
}

@Composable
private fun TemplatesGrid(
    predefined: List<Template>,
    custom: List<Template>,
    bottomContentPadding: Dp,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 8.dp,
            end = 12.dp,
            bottom = bottomContentPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        if (predefined.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
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
            item(span = StaggeredGridItemSpan.FullLine) {
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
    }
}

@Composable
private fun SectionHeader(title: String) {
    NexSectionLabel(
        text = title,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
