package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class RadialMenuStateTest {

    private val center          = Offset(500f, 500f)
    private val neutralRadius   = 30f
    private val selectionRadius = 150f // distance used in assertions

    // ── Neutral zone ──────────────────────────────────────────────────────────

    @Test
    fun `inside neutral zone returns -1`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x + 20f, center.y),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(-1, result)
    }

    @Test
    fun `exactly on center returns -1`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = center,
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(-1, result)
    }

    // ── 4 items (12 / 3 / 6 / 9 o'clock) ────────────────────────────────────

    @Test
    fun `up (12 o'clock) selects item 0`() {
        // Straight up → 12 o'clock
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x, center.y - selectionRadius),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(0, result)
    }

    @Test
    fun `right (3 o'clock) selects item 1`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x + selectionRadius, center.y),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(1, result)
    }

    @Test
    fun `down (6 o'clock) selects item 2`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x, center.y + selectionRadius),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(2, result)
    }

    @Test
    fun `left (9 o'clock) selects item 3`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x - selectionRadius, center.y),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(3, result)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `itemCount 0 returns -1`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x + selectionRadius, center.y),
            itemCount       = 0,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(-1, result)
    }

    @Test
    fun `itemCount 1 always returns 0 outside neutral zone`() {
        // With a single item any direction must select it
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(center.x + selectionRadius, center.y + selectionRadius),
            itemCount       = 1,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(0, result)
    }

    // ── Centering: half-sector shift ─────────────────────────────────────────

    @Test
    fun `slightly left of 12 o'clock still selects item 0`() {
        // Without the shift the boundary was at 0°: 1° left of top gave item 3.
        // With the shift the boundary is at -45° from top: item 0 covers [-45°, +45°].
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(
                center.x - (selectionRadius * Math.sin(Math.toRadians(20.0))).toFloat(),
                center.y - (selectionRadius * Math.cos(Math.toRadians(20.0))).toFloat()
            ),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(0, result)
    }

    @Test
    fun `slightly right of 12 o'clock still selects item 0`() {
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(
                center.x + (selectionRadius * Math.sin(Math.toRadians(20.0))).toFloat(),
                center.y - (selectionRadius * Math.cos(Math.toRadians(20.0))).toFloat()
            ),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius
        )
        assertEquals(0, result)
    }

    // ── Hysteresis ────────────────────────────────────────────────────────────

    @Test
    fun `hysteresis keeps current item a few degrees past the boundary`() {
        // 4° past the boundary between item 0 and 1 (HYSTERESIS_DEG = 8)
        val smallBeyondBoundary = 45.0 + 4.0
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(
                center.x + (selectionRadius * Math.sin(Math.toRadians(smallBeyondBoundary))).toFloat(),
                center.y - (selectionRadius * Math.cos(Math.toRadians(smallBeyondBoundary))).toFloat()
            ),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius,
            currentIndex    = 0
        )
        assertEquals("must stay on item 0 within the hysteresis zone", 0, result)
    }

    @Test
    fun `hysteresis allows transition once clearly past the threshold`() {
        // 15° past the boundary > HYSTERESIS_DEG = 8
        val clearlyInSector1 = 45.0 + 15.0
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(
                center.x + (selectionRadius * Math.sin(Math.toRadians(clearlyInSector1))).toFloat(),
                center.y - (selectionRadius * Math.cos(Math.toRadians(clearlyInSector1))).toFloat()
            ),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius,
            currentIndex    = 0
        )
        assertEquals("must select item 1 once past the threshold", 1, result)
    }

    @Test
    fun `hysteresis inactive when currentIndex is minus one`() {
        val justBeyondBoundary = 45.0 + 3.0
        val result = calculateSelectedIndex(
            center          = center,
            current         = Offset(
                center.x + (selectionRadius * Math.sin(Math.toRadians(justBeyondBoundary))).toFloat(),
                center.y - (selectionRadius * Math.cos(Math.toRadians(justBeyondBoundary))).toFloat()
            ),
            itemCount       = 4,
            neutralRadiusPx = neutralRadius,
            currentIndex    = -1
        )
        assertEquals("without currentIndex must select the real sector", 1, result)
    }

    // ── itemOffset ────────────────────────────────────────────────────────────

    @Test
    fun `itemOffset for item 0 points to 12 o'clock`() {
        val radius = 100f
        val offset = itemOffset(center, index = 0, itemCount = 4, radiusPx = radius)
        assertEquals(center.x, offset.x, 1f)
        assertEquals(center.y - radius, offset.y, 1f)
    }

    @Test
    fun `itemOffset for item 1 points right`() {
        val radius = 100f
        val offset = itemOffset(center, index = 1, itemCount = 4, radiusPx = radius)
        assertEquals(center.x + radius, offset.x, 1f)
        assertEquals(center.y, offset.y, 1f)
    }
}
