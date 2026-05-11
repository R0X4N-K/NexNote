package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorViewportMetricsTest {

    @Test
    fun `unobscuredViewportHeightPx subtracts bottom obstruction`() {
        val height = unobscuredViewportHeightPx(
            viewportHeightPx = 720,
            bottomObstructionHeightPx = 44
        )

        assertEquals(676, height)
    }

    @Test
    fun `unobscuredViewportHeightPx clamps collapsed viewport to one pixel`() {
        val height = unobscuredViewportHeightPx(
            viewportHeightPx = 32,
            bottomObstructionHeightPx = 64
        )

        assertEquals(1, height)
    }

    @Test
    fun `unobscuredViewportHeightPx ignores negative inputs`() {
        val height = unobscuredViewportHeightPx(
            viewportHeightPx = -10,
            bottomObstructionHeightPx = -20
        )

        assertEquals(1, height)
    }
}
