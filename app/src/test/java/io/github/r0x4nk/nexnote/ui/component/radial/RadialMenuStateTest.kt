package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class RadialMenuStateTest {

    private val center = Offset(500f, 500f)

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
