package io.github.r0x4nk.nexnote.ui.navigation

import io.github.r0x4nk.nexnote.domain.model.VaultState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultRecentPreviewProtectionPolicyTest {

    @Test
    fun `protects unlocked Vault route when preference is enabled`() {
        assertTrue(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                vaultState = VaultState.UNLOCKED,
                route = Screen.Vault.route,
                vaultNoteId = null
            )
        )
    }

    @Test
    fun `protects unlocked Vault editor route when preference is enabled`() {
        assertTrue(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                vaultState = VaultState.UNLOCKED,
                route = Screen.Editor.route,
                vaultNoteId = 42L
            )
        )
    }

    @Test
    fun `protects unlocked new Vault editor route when preference is enabled`() {
        assertTrue(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                vaultState = VaultState.UNLOCKED,
                route = Screen.Editor.route,
                vaultNoteId = Screen.Editor.NEW_VAULT_NOTE_ID
            )
        )
    }

    @Test
    fun `does not protect Vault route when Vault is locked`() {
        assertFalse(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                vaultState = VaultState.LOCKED,
                route = Screen.Vault.route,
                vaultNoteId = null
            )
        )
    }

    @Test
    fun `does not protect normal editor route`() {
        assertFalse(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                vaultState = VaultState.UNLOCKED,
                route = Screen.Editor.route,
                vaultNoteId = Screen.NO_ID
            )
        )
    }

    @Test
    fun `does not protect when preference is disabled`() {
        assertFalse(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = false,
                vaultState = VaultState.UNLOCKED,
                route = Screen.Vault.route,
                vaultNoteId = null
            )
        )
    }
}
