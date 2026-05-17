package io.github.r0x4nk.nexnote.ui.navigation

import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultAutoLockPolicyTest {

    @Test
    fun `locks unlocked Vault when immediate background lock is enabled`() {
        assertTrue(
            shouldAutoLockVaultOnStop(
                lockImmediatelyOnBackground = true,
                vaultState = VaultState.UNLOCKED,
                isChangingConfigurations = false
            )
        )
    }

    @Test
    fun `does not lock when Vault is already locked`() {
        assertFalse(
            shouldAutoLockVaultOnStop(
                lockImmediatelyOnBackground = true,
                vaultState = VaultState.LOCKED,
                isChangingConfigurations = false
            )
        )
    }

    @Test
    fun `does not lock when Vault is not configured`() {
        assertFalse(
            shouldAutoLockVaultOnStop(
                lockImmediatelyOnBackground = true,
                vaultState = VaultState.NOT_CONFIGURED,
                isChangingConfigurations = false
            )
        )
    }

    @Test
    fun `does not lock when immediate background lock is disabled`() {
        assertFalse(
            shouldAutoLockVaultOnStop(
                lockImmediatelyOnBackground = false,
                vaultState = VaultState.UNLOCKED,
                isChangingConfigurations = false
            )
        )
    }

    @Test
    fun `does not lock during configuration changes`() {
        assertFalse(
            shouldAutoLockVaultOnStop(
                lockImmediatelyOnBackground = true,
                vaultState = VaultState.UNLOCKED,
                isChangingConfigurations = true
            )
        )
    }

    @Test
    fun `locks unlocked Vault when screen turns off and immediate background lock is enabled`() {
        assertTrue(
            shouldAutoLockVaultOnScreenOff(
                lockImmediatelyOnBackground = true,
                vaultState = VaultState.UNLOCKED
            )
        )
    }

    @Test
    fun `does not lock on screen off when Vault is already locked`() {
        assertFalse(
            shouldAutoLockVaultOnScreenOff(
                lockImmediatelyOnBackground = true,
                vaultState = VaultState.LOCKED
            )
        )
    }

    @Test
    fun `does not lock on screen off when immediate background lock is disabled`() {
        assertFalse(
            shouldAutoLockVaultOnScreenOff(
                lockImmediatelyOnBackground = false,
                vaultState = VaultState.UNLOCKED
            )
        )
    }

    @Test
    fun `resume locks unlocked Vault when timeout is IMMEDIATELY and elapsed is zero`() {
        assertTrue(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.IMMEDIATELY,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = 0L
            )
        )
    }

    @Test
    fun `resume locks unlocked Vault when timeout is IMMEDIATELY and elapsed is large`() {
        assertTrue(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.IMMEDIATELY,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = 60 * 60_000L
            )
        )
    }

    @Test
    fun `resume never locks when timeout is NEVER`() {
        assertFalse(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.NEVER,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = 60 * 60_000L
            )
        )
    }

    @Test
    fun `resume locks when elapsed equals finite timeout duration`() {
        assertTrue(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.AFTER_5_MINUTES,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = 5 * 60_000L
            )
        )
    }

    @Test
    fun `resume locks when elapsed exceeds finite timeout duration`() {
        assertTrue(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.AFTER_1_MINUTE,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = 90_000L
            )
        )
    }

    @Test
    fun `resume does not lock when elapsed is below finite timeout duration`() {
        assertFalse(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.AFTER_15_MINUTES,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = 10 * 60_000L
            )
        )
    }

    @Test
    fun `resume does not lock when Vault is already locked`() {
        assertFalse(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.IMMEDIATELY,
                vaultState = VaultState.LOCKED,
                elapsedSinceBackgroundMillis = 60_000L
            )
        )
    }

    @Test
    fun `resume does not lock when Vault is not configured`() {
        assertFalse(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.AFTER_5_MINUTES,
                vaultState = VaultState.NOT_CONFIGURED,
                elapsedSinceBackgroundMillis = 60 * 60_000L
            )
        )
    }

    @Test
    fun `resume fails safe and locks when elapsed is negative`() {
        assertTrue(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.AFTER_30_MINUTES,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = -1L
            )
        )
    }

    @Test
    fun `resume with NEVER does not lock even when elapsed is negative`() {
        assertFalse(
            shouldAutoLockVaultOnResume(
                timeout = VaultAutoLockTimeout.NEVER,
                vaultState = VaultState.UNLOCKED,
                elapsedSinceBackgroundMillis = -1L
            )
        )
    }
}
