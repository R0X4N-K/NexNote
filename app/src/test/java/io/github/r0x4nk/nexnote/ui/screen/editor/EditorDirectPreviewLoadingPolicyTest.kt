package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDirectPreviewLoadingPolicyTest {

    @Test
    fun `shows loading while the first warmup is still pending`() {
        assertTrue(
            shouldShowDirectPreviewLoading(
                hasCompletedReveal = false,
                isWarmupPending = true
            )
        )
    }

    @Test
    fun `hides loading once the preview has been revealed`() {
        assertFalse(
            shouldShowDirectPreviewLoading(
                hasCompletedReveal = true,
                isWarmupPending = false
            )
        )
    }

    @Test
    fun `keeps loading hidden after a content edit re-keys the warmup`() {
        assertFalse(
            shouldShowDirectPreviewLoading(
                hasCompletedReveal = true,
                isWarmupPending = true
            )
        )
    }
}
