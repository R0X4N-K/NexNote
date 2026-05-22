package io.github.r0x4nk.nexnote.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultRecentPreviewProtectionPolicyTest {

    @Test
    fun `protects Vault route when preference is enabled`() {
        assertTrue(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                route = Screen.Vault.route,
                vaultNoteId = null
            )
        )
    }

    @Test
    fun `protects existing Vault editor route when preference is enabled`() {
        assertTrue(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                route = Screen.Editor.route,
                vaultNoteId = 42L
            )
        )
    }

    @Test
    fun `protects new Vault editor route when preference is enabled`() {
        assertTrue(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                route = Screen.Editor.route,
                vaultNoteId = Screen.Editor.NEW_VAULT_NOTE_ID
            )
        )
    }

    @Test
    fun `does not protect unknown route`() {
        assertFalse(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
                route = null,
                vaultNoteId = null
            )
        )
    }

    @Test
    fun `does not protect normal editor route`() {
        assertFalse(
            shouldProtectVaultRecentPreviews(
                protectVaultRecentPreviews = true,
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
                route = Screen.Vault.route,
                vaultNoteId = null
            )
        )
    }
}
