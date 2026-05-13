package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Non-scrollable masonry layout for grid content embedded inside another
 * scroll container. Children are measured at a fixed lane width and placed in
 * the currently shortest column, so uneven card heights do not leave row gaps.
 */
@Composable
internal fun MasonryGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val columnCount = columns.coerceAtLeast(1)

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalSpacingPx = horizontalSpacing.roundToPx()
        val verticalSpacingPx = verticalSpacing.roundToPx()
        val boundedWidth = constraints.maxWidth != Constraints.Infinity
        val availableWidth = if (boundedWidth) constraints.maxWidth else constraints.minWidth
        val totalSpacing = horizontalSpacingPx * (columnCount - 1)
        val columnWidth = ((availableWidth - totalSpacing) / columnCount).coerceAtLeast(0)
        val itemConstraints = constraints.copy(
            minWidth = columnWidth,
            maxWidth = columnWidth,
            minHeight = 0
        )
        val columnHeights = IntArray(columnCount)
        val placements = measurables.map { measurable ->
            val column = columnHeights.shortestColumnIndex()
            val placeable = measurable.measure(itemConstraints)
            val x = column * (columnWidth + horizontalSpacingPx)
            val y = columnHeights[column]
            columnHeights[column] += placeable.height + verticalSpacingPx
            MasonryPlacement(placeable, x, y)
        }

        val contentHeight = if (placements.isEmpty()) {
            0
        } else {
            (columnHeights.maxOrNull() ?: 0) - verticalSpacingPx
        }
        val width = if (boundedWidth) availableWidth else columnWidth * columnCount + totalSpacing
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            placements.forEach { placement ->
                placement.placeable.placeRelative(placement.x, placement.y)
            }
        }
    }
}

private data class MasonryPlacement(
    val placeable: Placeable,
    val x: Int,
    val y: Int
)

private fun IntArray.shortestColumnIndex(): Int {
    var shortestIndex = 0
    for (index in 1..lastIndex) {
        if (this[index] < this[shortestIndex]) {
            shortestIndex = index
        }
    }
    return shortestIndex
}
