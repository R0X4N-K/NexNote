package io.github.r0x4nk.nexnote.ui.screen.tags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalancedTreemapLayoutTest {

    @Test
    fun `tile areas are proportional to their weights`() {
        val width = 120f
        val height = 80f
        val weights = listOf(6, 3, 1)

        val tiles = calculateBalancedTreemap(weights, width, height)
        val totalArea = width * height
        val totalWeight = weights.sum().toFloat()

        weights.indices.forEach { index ->
            val expectedArea = totalArea * weights[index] / totalWeight
            assertEquals(expectedArea, tiles[index].area, 0.05f)
        }
    }

    @Test
    fun `tiles cover the chart without overlapping`() {
        val width = 320f
        val height = 180f
        val tiles = calculateBalancedTreemap(
            weights = listOf(13, 8, 5, 3, 2, 1),
            width = width,
            height = height
        )

        assertEquals(width * height, tiles.sumOf { it.area.toDouble() }.toFloat(), 0.1f)
        tiles.forEach { tile ->
            assertTrue(tile.left >= 0f)
            assertTrue(tile.top >= 0f)
            assertTrue(tile.right <= width)
            assertTrue(tile.bottom <= height)
            assertTrue(tile.width > 0f)
            assertTrue(tile.height > 0f)
        }
        tiles.indices.forEach { firstIndex ->
            ((firstIndex + 1) until tiles.size).forEach { secondIndex ->
                assertFalse(tiles[firstIndex].overlaps(tiles[secondIndex]))
            }
        }
    }

    @Test
    fun `zero weights still receive selectable area`() {
        val tiles = calculateBalancedTreemap(
            weights = listOf(0, 0, 0),
            width = 90f,
            height = 60f
        )

        assertEquals(3, tiles.size)
        tiles.forEach { tile -> assertTrue(tile.area > 0f) }
        assertEquals(90f * 60f, tiles.sumOf { it.area.toDouble() }.toFloat(), 0.05f)
    }
}

private fun TreemapRect.overlaps(other: TreemapRect): Boolean {
    val overlapWidth = minOf(right, other.right) - maxOf(left, other.left)
    val overlapHeight = minOf(bottom, other.bottom) - maxOf(top, other.top)
    return overlapWidth > 0.001f && overlapHeight > 0.001f
}
