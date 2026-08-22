package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import kotlin.math.roundToInt

@Composable
internal fun TagsTreemapList(
    tags: List<Tag>,
    maxCount: Int,
    selectedTagName: String?,
    notesForSelectedTag: List<Note>,
    actions: TagsActions,
    listState: LazyListState,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val selectedTag = remember(tags, selectedTagName) {
        tags.firstOrNull { tag -> tag.name == selectedTagName }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = bottomContentPadding + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "tag_treemap", contentType = "tag_treemap") {
            TagTreemapCard(
                tags = tags,
                selectedTagName = selectedTagName,
                onTagClick = actions.onTagClick
            )
        }
        selectedTag?.let { tag ->
            item(key = "selected_${tag.name}", contentType = "selected_tag") {
                TagScoreboardItem(
                    tag = tag,
                    maxCount = maxCount,
                    isExpanded = true,
                    notes = notesForSelectedTag,
                    onTagClick = { actions.onTagClick(tag.name) },
                    onNoteClick = actions.onNoteClick,
                    onRequestNoteActions = actions.onRequestNoteActions,
                    onDeleteClick = { actions.onDeleteClick(tag) }
                )
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun TagTreemapCard(
    tags: List<Tag>,
    selectedTagName: String?,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Tag usage",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Rectangle area represents the number of notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            TagTreemap(
                tags = tags,
                selectedTagName = selectedTagName,
                onTagClick = onTagClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
        }
    }
}

@Composable
private fun TagTreemap(
    tags: List<Tag>,
    selectedTagName: String?,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chartShape = MaterialTheme.shapes.medium
    Layout(
        content = {
            tags.forEach { tag ->
                TagTreemapTile(
                    tag = tag,
                    isSelected = tag.name == selectedTagName,
                    onClick = { onTagClick(tag.name) }
                )
            }
        },
        modifier = modifier
            .clip(chartShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = "Tag usage treemap" }
    ) { measurables, constraints ->
        val chartWidth = constraints.maxWidth
        val chartHeight = constraints.maxHeight
        val bounds = calculateBalancedTreemap(
            weights = tags.map { tag -> tag.noteCount },
            width = chartWidth.toFloat(),
            height = chartHeight.toFloat()
        )
        val placeables = measurables.mapIndexed { index, measurable ->
            val tile = bounds[index]
            val left = tile.left.roundToInt()
            val top = tile.top.roundToInt()
            val right = tile.right.roundToInt()
            val bottom = tile.bottom.roundToInt()
            val width = (right - left).coerceAtLeast(1)
            val height = (bottom - top).coerceAtLeast(1)
            measurable.measure(Constraints.fixed(width, height)) to (left to top)
        }

        layout(chartWidth, chartHeight) {
            placeables.forEach { (placeable, position) ->
                placeable.placeRelative(position.first, position.second)
            }
        }
    }
}

@Composable
private fun TagTreemapTile(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = tagTreemapTileColors(tag, isSelected)
    val noteLabel = if (tag.noteCount == 1) "note" else "notes"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "#${tag.name}, ${tag.noteCount} $noteLabel"
                    selected = isSelected
                },
            shape = MaterialTheme.shapes.small,
            color = colors.container,
            contentColor = colors.content,
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            }
        ) {
            TagTreemapTileLabel(tag)
        }
    }
}

@Composable
private fun TagTreemapTileLabel(tag: Tag) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when {
            maxWidth >= 72.dp && maxHeight >= 52.dp -> {
                Column {
                    Text(
                        text = "#${tag.name}",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tag.noteCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            maxWidth >= 48.dp && maxHeight >= 30.dp -> {
                Text(
                    text = "#${tag.name}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class TagTreemapTileColors(
    val container: Color,
    val content: Color
)

@Composable
private fun tagTreemapTileColors(
    tag: Tag,
    isSelected: Boolean
): TagTreemapTileColors {
    val colorScheme = MaterialTheme.colorScheme
    if (isSelected) {
        return TagTreemapTileColors(
            container = colorScheme.primaryContainer,
            content = colorScheme.onPrimaryContainer
        )
    }

    val palette = listOf(
        TagTreemapTileColors(colorScheme.primaryContainer, colorScheme.onPrimaryContainer),
        TagTreemapTileColors(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer),
        TagTreemapTileColors(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer),
        TagTreemapTileColors(colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
    )
    val paletteIndex = (tag.name.hashCode() and Int.MAX_VALUE) % palette.size
    return palette[paletteIndex]
}
