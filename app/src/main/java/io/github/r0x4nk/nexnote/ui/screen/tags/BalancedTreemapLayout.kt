package io.github.r0x4nk.nexnote.ui.screen.tags

import kotlin.math.abs

/** Pixel bounds assigned to one item in a treemap. */
internal data class TreemapRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height
}

private data class WeightedTreemapItem(
    val index: Int,
    val weight: Double
)

private data class TreemapPartition(
    val items: List<WeightedTreemapItem>,
    val totalWeight: Double,
    val bounds: TreemapRect
)

/**
 * Divides [width] x [height] into non-overlapping rectangles whose areas are
 * proportional to [weights]. Zero weights receive a minimum unit so that every
 * tag remains visible and selectable.
 */
internal fun calculateBalancedTreemap(
    weights: List<Int>,
    width: Float,
    height: Float
): List<TreemapRect> {
    if (weights.isEmpty() || width <= 0f || height <= 0f) return emptyList()

    val items = weights.mapIndexed { index, weight ->
        WeightedTreemapItem(
            index = index,
            weight = weight.coerceAtLeast(TREEMAP_MIN_WEIGHT).toDouble()
        )
    }
    val result = MutableList<TreemapRect?>(items.size) { null }
    val pending = mutableListOf(
        TreemapPartition(
            items = items,
            totalWeight = items.sumOf { it.weight },
            bounds = TreemapRect(0f, 0f, width, height)
        )
    )

    while (pending.isNotEmpty()) {
        val partition = pending.removeAt(pending.lastIndex)
        if (partition.items.size == 1) {
            result[partition.items.single().index] = partition.bounds
            continue
        }

        val splitIndex = findBalancedSplit(partition.items, partition.totalWeight)
        val firstItems = partition.items.subList(0, splitIndex)
        val secondItems = partition.items.subList(splitIndex, partition.items.size)
        val firstWeight = firstItems.sumOf { it.weight }
        val secondWeight = partition.totalWeight - firstWeight
        val (firstBounds, secondBounds) = splitBounds(
            bounds = partition.bounds,
            firstFraction = (firstWeight / partition.totalWeight).toFloat()
        )

        pending += TreemapPartition(secondItems, secondWeight, secondBounds)
        pending += TreemapPartition(firstItems, firstWeight, firstBounds)
    }

    return result.map { bounds -> checkNotNull(bounds) }
}

private fun findBalancedSplit(
    items: List<WeightedTreemapItem>,
    totalWeight: Double
): Int {
    val targetWeight = totalWeight / 2.0
    var accumulatedWeight = 0.0
    var bestSplit = 1
    var bestDistance = Double.POSITIVE_INFINITY

    for (index in 1 until items.size) {
        accumulatedWeight += items[index - 1].weight
        val distance = abs(targetWeight - accumulatedWeight)
        if (distance < bestDistance) {
            bestDistance = distance
            bestSplit = index
        }
    }
    return bestSplit
}

private fun splitBounds(
    bounds: TreemapRect,
    firstFraction: Float
): Pair<TreemapRect, TreemapRect> {
    return if (bounds.width >= bounds.height) {
        val splitX = bounds.left + bounds.width * firstFraction
        TreemapRect(bounds.left, bounds.top, splitX, bounds.bottom) to
            TreemapRect(splitX, bounds.top, bounds.right, bounds.bottom)
    } else {
        val splitY = bounds.top + bounds.height * firstFraction
        TreemapRect(bounds.left, bounds.top, bounds.right, splitY) to
            TreemapRect(bounds.left, splitY, bounds.right, bounds.bottom)
    }
}
