package io.github.r0x4nk.nexnote.ui.navigation

import io.github.r0x4nk.nexnote.domain.model.VaultState

internal fun shouldProtectVaultRecentPreviews(
    protectVaultRecentPreviews: Boolean,
    vaultState: VaultState,
    route: String?,
    vaultNoteId: Long?
): Boolean {
    if (!protectVaultRecentPreviews || vaultState != VaultState.UNLOCKED) return false

    return when (route) {
        Screen.Vault.route -> true
        Screen.Editor.route -> vaultNoteId != null && vaultNoteId != Screen.NO_ID
        else -> false
    }
}
