package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.VaultState
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    val state: Flow<VaultState>
    val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean>

    suspend fun configurePin(pin: CharArray)
    suspend fun unlockWithPin(pin: CharArray): Boolean

    /**
     * Unlock the Vault using Android credential-protected material that was
     * previously refreshed after a successful PIN unlock.
     *
     * Callers must request Android credential confirmation before invoking
     * this operation. The data layer still relies on Android Keystore to
     * enforce that a valid credential authentication happened recently enough;
     * this method never accepts or persists a PIN.
     */
    suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult

    /**
     * Refresh the Android credential-protected unlock material from the
     * currently unlocked Vault key.
     *
     * Callers must request Android credential confirmation before invoking
     * this operation. The Vault key remains confined to the data layer and the
     * operation persists only a Keystore-protected envelope.
     */
    suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult

    /**
     * Remove any persisted Android credential-protected Vault unlock material.
     *
     * This clears only the non-plaintext Keystore envelope used as an alternate
     * unlock path. It must not affect the configured PIN, encrypted Vault notes,
     * or the in-memory unlocked Vault key.
     */
    suspend fun clearAndroidCredentialProtectedUnlockMaterial()

    /**
     * Replace the current Vault PIN with [newPin], re-encrypting every Vault
     * note under a freshly derived key.
     *
     * The operation never accepts the new PIN without proof of the current
     * one: callers must provide the current PIN, which is verified against the
     * stored verifier before any change happens. The Vault must already be
     * unlocked, otherwise [ChangeVaultPinResult.VaultLocked] is returned.
     *
     * Implementations must re-encrypt existing Vault notes with the new key
     * before overwriting the persisted PIN verifier and key derivation
     * parameters, so that a failure during re-encryption leaves the Vault in
     * a coherent state and the previous PIN keeps working. Neither PIN must
     * ever be persisted in clear or kept in UI state.
     */
    suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult

    /**
     * Permanently reset the Vault. This is a destructive operation: every
     * Vault note row is removed and every persisted Vault material (PIN
     * verifier, key derivation parameters, Android Keystore unlock envelope)
     * is cleared. The in-memory unlocked key, if any, is discarded.
     *
     * Reset requires the Vault to already be unlocked, so the destructive
     * operation cannot be triggered from a locked state without prior
     * authentication. Implementations must not log, persist, or expose any
     * PIN or key material.
     *
     * Normal notes (active or in the trash) must be left untouched. User
     * preferences such as Android screen-lock unlock enablement or auto-lock
     * timeout are not modified by this contract; coordinating those is the
     * responsibility of the caller in subsequent steps.
     */
    suspend fun resetVault(): ResetVaultResult

    fun lock()
}

/**
 * Outcome of [VaultRepository.resetVault]. Explicit results keep the UI from
 * having to inspect exceptions and prevent leaking PIN or key material via
 * error messages.
 */
sealed interface ResetVaultResult {
    /** The Vault has been wiped and all persisted material has been cleared. */
    object Success : ResetVaultResult

    /** No Vault is configured, so there is nothing to reset. */
    object VaultNotConfigured : ResetVaultResult

    /** The Vault is configured but locked, so reset is not allowed yet. */
    object VaultLocked : ResetVaultResult

    /**
     * The reset could not be completed. Implementations must leave the Vault
     * in a coherent state: either the previous configuration is preserved, or
     * the Vault is at least locked so that no stale unlocked state remains.
     */
    object Failed : ResetVaultResult
}

/**
 * Outcome of [VaultRepository.changePin]. Intentionally explicit so the UI can
 * react without inspecting exceptions and without leaking content of either
 * PIN through error messages.
 */
sealed interface ChangeVaultPinResult {
    /** The Vault has been re-keyed with the provided new PIN. */
    object Success : ChangeVaultPinResult

    /** No Vault is configured yet, so there is no PIN to change. */
    object VaultNotConfigured : ChangeVaultPinResult

    /** The Vault is configured but currently locked. */
    object VaultLocked : ChangeVaultPinResult

    /** The supplied current PIN does not match the stored verifier. */
    object WrongCurrentPin : ChangeVaultPinResult

    /** The supplied new PIN is not acceptable (e.g. empty). */
    object InvalidNewPin : ChangeVaultPinResult

    /**
     * Re-encryption of existing Vault notes failed. The Vault keeps the
     * previous PIN, key and persisted verifier untouched.
     */
    object RewrapFailed : ChangeVaultPinResult
}

/**
 * Outcome of Android credential-based Vault unlock. The result intentionally
 * separates prompt/authentication state from Vault state so callers can keep
 * errors non-sensitive and avoid guessing from exceptions.
 */
sealed interface UnlockVaultWithAndroidCredentialResult {
    /** The protected Vault key was recovered and is now held only in memory. */
    object Success : UnlockVaultWithAndroidCredentialResult

    /** No Vault is configured yet. */
    object VaultNotConfigured : UnlockVaultWithAndroidCredentialResult

    /** No Android credential-protected unlock material is available. */
    object NoProtectedMaterial : UnlockVaultWithAndroidCredentialResult

    /** Android secure lock screen credentials are unavailable. */
    object CredentialUnavailable : UnlockVaultWithAndroidCredentialResult

    /** Android Keystore still requires a fresh user authentication. */
    object AuthenticationRequired : UnlockVaultWithAndroidCredentialResult

    /** The Keystore key was invalidated, for example by credential changes. */
    object KeyInvalidated : UnlockVaultWithAndroidCredentialResult

    /** The stored protected material is malformed or cannot be trusted. */
    object InvalidPayload : UnlockVaultWithAndroidCredentialResult

    /** A generic non-sensitive failure occurred. */
    object Failed : UnlockVaultWithAndroidCredentialResult
}

/**
 * Outcome of refreshing Android credential-protected unlock material from the
 * already unlocked Vault key. Results avoid exceptions so UI layers can keep
 * messages non-sensitive.
 */
sealed interface RefreshVaultAndroidCredentialProtectedMaterialResult {
    /** The current Vault key was wrapped into a fresh protected envelope. */
    object Success : RefreshVaultAndroidCredentialProtectedMaterialResult

    /** The Vault key is not currently unlocked in memory. */
    object VaultLocked : RefreshVaultAndroidCredentialProtectedMaterialResult

    /** Android secure lock screen credentials are unavailable. */
    object CredentialUnavailable : RefreshVaultAndroidCredentialProtectedMaterialResult

    /** Android Keystore still requires a fresh user authentication. */
    object AuthenticationRequired : RefreshVaultAndroidCredentialProtectedMaterialResult

    /** The Keystore key was invalidated, for example by credential changes. */
    object KeyInvalidated : RefreshVaultAndroidCredentialProtectedMaterialResult

    /** A generic non-sensitive failure occurred. */
    object Failed : RefreshVaultAndroidCredentialProtectedMaterialResult
}
