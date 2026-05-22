package io.github.r0x4nk.nexnote.ui.navigation

internal fun shouldProtectVaultRecentPreviews(
    protectVaultRecentPreviews: Boolean,
    route: String?,
    vaultNoteId: Long?
): Boolean {
    if (!protectVaultRecentPreviews) return false

    return when (route) {
        Screen.Vault.route -> true
        Screen.Editor.route -> vaultNoteId != null && vaultNoteId != Screen.NO_ID
        else -> false
    }
}
