package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton

@Composable
internal fun TemplatesCollection(
    uiState: TemplatesUiState,
    padding: PaddingValues,
    floatingBottomPadding: Dp,
    selectionState: SelectionUiState,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    onToggleSelection: (Template) -> Unit
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val bottomContentPadding = RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)
    val scrollToTopBottomPadding = if (selectionState.isActive) {
        floatingBottomPadding + 16.dp
    } else {
        bottomContentPadding
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (uiState.viewMode == NoteListViewMode.GRID) {
            TemplatesGrid(
                predefined = uiState.predefined,
                custom = uiState.custom,
                bottomContentPadding = bottomContentPadding,
                gridState = gridState,
                selectionState = selectionState,
                onApply = onApply,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleSelection = onToggleSelection,
                modifier = Modifier.fillMaxSize()
            )
            ScrollToTopButton(
                gridState = gridState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = scrollToTopBottomPadding)
            )
        } else {
            TemplatesList(
                predefined = uiState.predefined,
                custom = uiState.custom,
                bottomContentPadding = bottomContentPadding,
                listState = listState,
                selectionState = selectionState,
                onApply = onApply,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleSelection = onToggleSelection,
                modifier = Modifier.fillMaxSize()
            )
            ScrollToTopButton(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = scrollToTopBottomPadding)
            )
        }
    }
}

@Composable
private fun TemplatesList(
    predefined: List<Template>,
    custom: List<Template>,
    bottomContentPadding: Dp,
    listState: LazyListState,
    selectionState: SelectionUiState,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    onToggleSelection: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
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
            item {
                SectionHeader(
                    title = "Built-in",
                    description = "Ready-to-use starting points",
                    count = predefined.size
                )
            }
            items(predefined, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = {
                        if (selectionState.isActive) {
                            onToggleSelection(template)
                        } else {
                            onApply(template.id)
                        }
                    },
                    onEdit = null,
                    onDelete = { onDelete(template) },
                    onLongPress = { onToggleSelection(template) },
                    selectionMode = selectionState.isActive,
                    selected = selectionState.isSelected(template.id)
                )
            }
        }

        if (custom.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Your templates",
                    description = "Structures you can edit and reuse",
                    count = custom.size
                )
            }
            items(custom, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = {
                        if (selectionState.isActive) {
                            onToggleSelection(template)
                        } else {
                            onApply(template.id)
                        }
                    },
                    onEdit = { onEdit(template.id) },
                    onDelete = { onDelete(template) },
                    onLongPress = { onToggleSelection(template) },
                    selectionMode = selectionState.isActive,
                    selected = selectionState.isSelected(template.id)
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
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Template) -> Unit,
    onToggleSelection: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
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
                SectionHeader(
                    title = "Built-in",
                    description = "Ready-to-use starting points",
                    count = predefined.size
                )
            }
            items(predefined, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = {
                        if (selectionState.isActive) {
                            onToggleSelection(template)
                        } else {
                            onApply(template.id)
                        }
                    },
                    onEdit = null,
                    onDelete = { onDelete(template) },
                    onLongPress = { onToggleSelection(template) },
                    selectionMode = selectionState.isActive,
                    selected = selectionState.isSelected(template.id)
                )
            }
        }

        if (custom.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(
                    title = "Your templates",
                    description = "Structures you can edit and reuse",
                    count = custom.size
                )
            }
            items(custom, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onApply = {
                        if (selectionState.isActive) {
                            onToggleSelection(template)
                        } else {
                            onApply(template.id)
                        }
                    },
                    onEdit = { onEdit(template.id) },
                    onDelete = { onDelete(template) },
                    onLongPress = { onToggleSelection(template) },
                    selectionMode = selectionState.isActive,
                    selected = selectionState.isSelected(template.id)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, description: String, count: Int) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
