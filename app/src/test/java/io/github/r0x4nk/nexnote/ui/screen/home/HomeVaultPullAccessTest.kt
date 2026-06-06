package io.github.r0x4nk.nexnote.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeVaultPullAccessTest {

    @Test
    fun indicatorStateBeforeThresholdPromptsToKeepPulling() {
        val state = homeVaultPullIndicatorState(
            pullOffsetPx = 64f,
            thresholdPx = 100f
        )

        assertEquals(0.64f, state.progress, 0.001f)
        assertFalse(state.thresholdReached)
        assertEquals(HOME_VAULT_PULL_IDLE_TEXT, state.text)
    }

    @Test
    fun indicatorStateAtThresholdPromptsToRelease() {
        val state = homeVaultPullIndicatorState(
            pullOffsetPx = 100f,
            thresholdPx = 100f
        )

        assertEquals(1f, state.progress, 0.001f)
        assertTrue(state.thresholdReached)
        assertEquals(HOME_VAULT_PULL_READY_TEXT, state.text)
    }

    @Test
    fun releaseOpensVaultOnlyAfterThreshold() {
        assertFalse(
            shouldOpenVaultOnPullRelease(
                pullOffsetPx = 99f,
                thresholdPx = 100f
            )
        )
        assertTrue(
            shouldOpenVaultOnPullRelease(
                pullOffsetPx = 100f,
                thresholdPx = 100f
            )
        )
    }

    @Test
    fun pullOffsetClampsToMaxReveal() {
        val offset = calculateHomeVaultPullOffset(
            currentOffsetPx = 95f,
            deltaPx = 200f,
            maxOffsetPx = 100f
        )

        assertEquals(100f, offset, 0.001f)
    }

    @Test
    fun upwardDragCollapsesWithoutGoingNegative() {
        val offset = calculateHomeVaultPullOffset(
            currentOffsetPx = 24f,
            deltaPx = -40f,
            maxOffsetPx = 100f
        )

        assertEquals(0f, offset, 0.001f)
    }
}
