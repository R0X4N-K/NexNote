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

    @Test
    fun indicatorVisualFeedbackScalesWithProgress() {
        val state = homeVaultPullIndicatorState(
            pullOffsetPx = 50f,
            thresholdPx = 100f
        )

        assertEquals(0.675f, state.contentAlpha, 0.001f)
        assertEquals(1.06f, state.iconScale, 0.001f)
        assertEquals(1.0125f, state.textScale, 0.001f)
        assertEquals(17f, state.contentGapDp, 0.001f)
    }

    @Test
    fun indicatorVisualFeedbackClampsAtThreshold() {
        val state = homeVaultPullIndicatorState(
            pullOffsetPx = 150f,
            thresholdPx = 100f
        )

        assertEquals(1f, state.contentAlpha, 0.001f)
        assertEquals(1.12f, state.iconScale, 0.001f)
        assertEquals(1.025f, state.textScale, 0.001f)
        assertEquals(20f, state.contentGapDp, 0.001f)
    }

    @Test
    fun visualFeedbackUsesSmoothedProgress() {
        assertEquals(0f, homeVaultPullSmoothedProgress(-1f), 0.001f)
        assertEquals(0.15625f, homeVaultPullSmoothedProgress(0.25f), 0.001f)
        assertEquals(0.84375f, homeVaultPullSmoothedProgress(0.75f), 0.001f)
        assertEquals(1f, homeVaultPullSmoothedProgress(2f), 0.001f)
    }
}
