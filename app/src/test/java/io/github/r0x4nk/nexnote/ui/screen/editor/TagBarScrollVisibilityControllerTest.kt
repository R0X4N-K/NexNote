package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagBarScrollVisibilityControllerTest {

    @Test
    fun `moving text upward hides the tag bar after threshold`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 0,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        assertNull(
            controller.onScrollChanged(
                currentScroll = 4,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
        assertEquals(
            TagBarVisibilityRequest.Hide,
            controller.onScrollChanged(
                currentScroll = 10,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `moving text downward shows the tag bar after threshold`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 100,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        assertNull(
            controller.onScrollChanged(
                currentScroll = 96,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
        assertEquals(
            TagBarVisibilityRequest.Show,
            controller.onScrollChanged(
                currentScroll = 90,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `moving text downward uses the reveal threshold when it is lower`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 100,
            initialMaxScroll = 100,
            thresholdPx = 28,
            revealThresholdPx = 10
        )

        assertEquals(
            TagBarVisibilityRequest.Show,
            controller.onScrollChanged(
                currentScroll = 90,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `direction changes are handled inside the same scroll gesture`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 100,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        assertEquals(
            TagBarVisibilityRequest.Show,
            controller.onScrollChanged(
                currentScroll = 88,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
        assertEquals(
            TagBarVisibilityRequest.Hide,
            controller.onScrollChanged(
                currentScroll = 100,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `idle scroll state resets accumulated distance`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 0,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        assertNull(
            controller.onScrollChanged(
                currentScroll = 6,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
        assertNull(
            controller.onScrollChanged(
                currentScroll = 6,
                currentMaxScroll = 100,
                isScrollInProgress = false
            )
        )
        assertNull(
            controller.onScrollChanged(
                currentScroll = 9,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `syncTo ignores scroll jumps caused by disabled auto visibility`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 0,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        controller.syncTo(currentScroll = 100, currentMaxScroll = 100)

        assertNull(
            controller.onScrollChanged(
                currentScroll = 106,
                currentMaxScroll = 120,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `max scroll decrease at bottom does not show the tag bar`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 100,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        assertNull(
            controller.onScrollChanged(
                currentScroll = 72,
                currentMaxScroll = 72,
                isScrollInProgress = true
            )
        )
    }

    @Test
    fun `max scroll changes reset accumulated gesture distance`() {
        val controller = TagBarScrollVisibilityController(
            initialScroll = 0,
            initialMaxScroll = 100,
            thresholdPx = 10
        )

        assertNull(
            controller.onScrollChanged(
                currentScroll = 6,
                currentMaxScroll = 100,
                isScrollInProgress = true
            )
        )
        assertNull(
            controller.onScrollChanged(
                currentScroll = 6,
                currentMaxScroll = 80,
                isScrollInProgress = true
            )
        )
        assertNull(
            controller.onScrollChanged(
                currentScroll = 9,
                currentMaxScroll = 80,
                isScrollInProgress = true
            )
        )
    }
}
