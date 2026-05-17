package io.github.r0x4nk.nexnote.ui.navigation

import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState

internal fun shouldAutoLockVaultOnStop(
    lockImmediatelyOnBackground: Boolean,
    vaultState: VaultState,
    isChangingConfigurations: Boolean
): Boolean {
    return lockImmediatelyOnBackground &&
        vaultState == VaultState.UNLOCKED &&
        !isChangingConfigurations
}

internal fun shouldAutoLockVaultOnScreenOff(
    lockImmediatelyOnBackground: Boolean,
    vaultState: VaultState
): Boolean {
    return lockImmediatelyOnBackground && vaultState == VaultState.UNLOCKED
}

/**
 * Pure policy: decide whether an unlocked Vault should be auto-locked when the app
 * resumes from background, based on the configured [VaultAutoLockTimeout] and the
 * time elapsed since the app went to background.
 *
 * Contract:
 * - Returns false unless [vaultState] is [VaultState.UNLOCKED]; there is nothing to
 *   lock otherwise and no Vault content can leak through this path.
 * - [VaultAutoLockTimeout.NEVER] (null [VaultAutoLockTimeout.durationMillis]) never
 *   triggers an auto-lock from this policy. The Vault can still be locked manually
 *   or by other policies (e.g. immediate background lock, screen off).
 * - [VaultAutoLockTimeout.IMMEDIATELY] (durationMillis == 0) triggers a lock for any
 *   non-negative elapsed value, matching the "lock on resume" semantic for the most
 *   restrictive setting.
 * - For finite timeouts (1, 5, 15, 30 minutes) the Vault is locked when the elapsed
 *   time meets or exceeds [VaultAutoLockTimeout.durationMillis].
 * - Negative [elapsedSinceBackgroundMillis] is treated as a clock anomaly and fails
 *   safe to locking the Vault, so no protected content is exposed if the elapsed
 *   time cannot be trusted.
 *
 * This function does not read PIN, keys or Vault content and produces no side
 * effects. The actual lock action is performed by the caller.
 */
internal fun shouldAutoLockVaultOnResume(
    timeout: VaultAutoLockTimeout,
    vaultState: VaultState,
    elapsedSinceBackgroundMillis: Long
): Boolean {
    if (vaultState != VaultState.UNLOCKED) return false
    val durationMillis = timeout.durationMillis ?: return false
    if (elapsedSinceBackgroundMillis < 0L) return true
    return elapsedSinceBackgroundMillis >= durationMillis
}
