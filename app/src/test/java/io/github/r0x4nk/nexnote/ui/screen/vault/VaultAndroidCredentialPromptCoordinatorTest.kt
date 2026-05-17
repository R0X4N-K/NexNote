package io.github.r0x4nk.nexnote.ui.screen.vault

import android.app.Activity
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialPromptResult
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultAndroidCredentialPromptCoordinatorTest {

    @Test
    fun `prompt launches only for a positive pending request`() {
        assertEquals(true, shouldLaunchAndroidCredentialPrompt(1L, true))
    }

    @Test
    fun `prompt does not relaunch for a consumed request id`() {
        assertEquals(false, shouldLaunchAndroidCredentialPrompt(1L, false))
    }

    @Test
    fun `prompt does not launch for pending zero request id`() {
        assertEquals(false, shouldLaunchAndroidCredentialPrompt(0L, true))
    }

    @Test
    fun `result mapper treats RESULT_OK as authenticated`() {
        assertEquals(
            VaultAndroidCredentialPromptResult.AUTHENTICATED,
            VaultAndroidCredentialPromptResultMapper.fromResultCode(Activity.RESULT_OK)
        )
    }

    @Test
    fun `result mapper treats non OK result as canceled`() {
        assertEquals(
            VaultAndroidCredentialPromptResult.CANCELED,
            VaultAndroidCredentialPromptResultMapper.fromResultCode(Activity.RESULT_CANCELED)
        )
    }
}
