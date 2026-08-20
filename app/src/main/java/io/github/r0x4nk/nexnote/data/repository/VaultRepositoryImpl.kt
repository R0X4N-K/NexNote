package io.github.r0x4nk.nexnote.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.r0x4nk.nexnote.data.security.AndroidVaultUnlockMaterialProtector
import io.github.r0x4nk.nexnote.data.security.ProtectVaultUnlockMaterialResult
import io.github.r0x4nk.nexnote.data.security.UnprotectVaultUnlockMaterialResult
import io.github.r0x4nk.nexnote.data.security.VaultKeyDerivationParams
import io.github.r0x4nk.nexnote.data.security.VaultKeyDeriver
import io.github.r0x4nk.nexnote.data.security.VaultPinHash
import io.github.r0x4nk.nexnote.data.security.VaultPinHasher
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vault_preferences"
)

private val Flow<Preferences>.safeVaultPreferences: Flow<Preferences>
    get() = catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

internal interface VaultUnlockedKeyProvider {
    val unlockedVaultKey: StateFlow<SecretKey?>

    suspend fun <T> withUnlockedVaultKey(block: suspend (SecretKey) -> T): T?
}

class VaultRepositoryImpl internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val pinHasher: VaultPinHasher = VaultPinHasher(),
    private val keyDeriver: VaultKeyDeriver = VaultKeyDeriver(),
    private val protectUnlockMaterial: ((ByteArray) -> ProtectVaultUnlockMaterialResult)? = null,
    private val unprotectUnlockMaterial: ((String) -> UnprotectVaultUnlockMaterialResult)? = null
) : VaultRepository, VaultUnlockedKeyProvider {

    constructor(context: Context) : this(
        dataStore = context.vaultDataStore,
        protectUnlockMaterial = AndroidVaultUnlockMaterialProtector(context)::protect,
        unprotectUnlockMaterial = AndroidVaultUnlockMaterialProtector(context)::unprotect
    )

    private val unlockedKey = MutableStateFlow<SecretKey?>(null)
    private val keyAccessMutex = Mutex()
    private lateinit var noteRewrapper: VaultNoteRewrapper
    private lateinit var noteWiper: VaultNoteWiper
    override val unlockedVaultKey: StateFlow<SecretKey?> = unlockedKey.asStateFlow()

    /**
     * Completes the Vault object graph after the note repository has received
     * this instance as its unlocked-key provider. Binding is one-shot and must
     * happen before the repository is exposed to callers.
     */
    internal fun bindNoteMaintenance(
        rewrapper: VaultNoteRewrapper,
        wiper: VaultNoteWiper
    ) {
        check(!::noteRewrapper.isInitialized && !::noteWiper.isInitialized) {
            "Vault note maintenance is already bound"
        }
        noteRewrapper = rewrapper
        noteWiper = wiper
    }

    private val storedConfig: Flow<VaultStoredConfig?> =
        dataStore.data
            .safeVaultPreferences
            .map { prefs -> prefs.toVaultStoredConfigOrNull() }
            .distinctUntilChanged()

    override val state: Flow<VaultState> =
        combine(storedConfig, unlockedKey) { config, key ->
            when {
                config == null -> VaultState.NOT_CONFIGURED
                key != null -> VaultState.UNLOCKED
                else -> VaultState.LOCKED
            }
        }.distinctUntilChanged()

    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        dataStore.data
            .safeVaultPreferences
            .map { prefs ->
                prefs[ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY]
                    ?.isNotBlank() == true
            }
            .distinctUntilChanged()

    override suspend fun configurePin(pin: CharArray) {
        val pinHash = pinHasher.hash(pin)
        val keyParams = keyDeriver.newParams()
        dataStore.edit { prefs ->
            prefs[PIN_ALGORITHM_KEY] = pinHash.algorithm
            prefs[PIN_ITERATIONS_KEY] = pinHash.iterations
            prefs[PIN_SALT_KEY] = pinHash.saltBase64
            prefs[PIN_HASH_KEY] = pinHash.hashBase64
            prefs[KEY_DERIVATION_ALGORITHM_KEY] = keyParams.algorithm
            prefs[KEY_DERIVATION_ITERATIONS_KEY] = keyParams.iterations
            prefs[KEY_DERIVATION_SALT_KEY] = keyParams.saltBase64
            prefs[KEY_DERIVATION_KEY_LENGTH_BITS_KEY] = keyParams.keyLengthBits
            prefs.remove(ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY)
        }
        unlockedKey.value = null
    }

    override suspend fun unlockWithPin(pin: CharArray): Boolean {
        val config = dataStore.data
            .safeVaultPreferences
            .map { prefs -> prefs.toVaultStoredConfigOrNull() }
            .first()
            ?: run {
                unlockedKey.value = null
                return false
            }

        if (!pinHasher.verify(pin, config.pinHash)) {
            unlockedKey.value = null
            return false
        }

        return runCatchingPreservingCancellation {
            val key = keyDeriver.deriveKey(pin, config.keyParams)
            unlockedKey.value = key
            refreshAndroidCredentialProtectedUnlockMaterial(
                key = key,
                clearStoredMaterialOnFailure = false
            )
            true
        }.getOrElse {
            unlockedKey.value = null
            false
        }
    }

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult {
        val prefs = dataStore.data
            .safeVaultPreferences
            .first()

        val config = prefs.toVaultStoredConfigOrNull()
            ?: run {
                unlockedKey.value = null
                return UnlockVaultWithAndroidCredentialResult.VaultNotConfigured
            }

        if (unlockedKey.value != null) {
            return UnlockVaultWithAndroidCredentialResult.Success
        }

        val encodedMaterial = prefs[ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY]
            ?.takeIf { it.isNotBlank() }
            ?: return UnlockVaultWithAndroidCredentialResult.NoProtectedMaterial

        val unprotector = unprotectUnlockMaterial
            ?: return UnlockVaultWithAndroidCredentialResult.Failed

        return try {
            when (val result = unprotector(encodedMaterial)) {
                is UnprotectVaultUnlockMaterialResult.Success ->
                    unlockWithRecoveredAndroidCredentialMaterial(result.material, config)

                UnprotectVaultUnlockMaterialResult.CredentialUnavailable -> {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                    UnlockVaultWithAndroidCredentialResult.CredentialUnavailable
                }

                UnprotectVaultUnlockMaterialResult.AuthenticationRequired ->
                    UnlockVaultWithAndroidCredentialResult.AuthenticationRequired

                UnprotectVaultUnlockMaterialResult.KeyInvalidated -> {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                    UnlockVaultWithAndroidCredentialResult.KeyInvalidated
                }

                UnprotectVaultUnlockMaterialResult.InvalidPayload -> {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                    UnlockVaultWithAndroidCredentialResult.InvalidPayload
                }

                UnprotectVaultUnlockMaterialResult.Failed ->
                    UnlockVaultWithAndroidCredentialResult.Failed
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            UnlockVaultWithAndroidCredentialResult.Failed
        } catch (error: Exception) {
            UnlockVaultWithAndroidCredentialResult.Failed
        }
    }

    override fun lock() {
        unlockedKey.value = null
    }

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() {
        clearStoredAndroidCredentialProtectedUnlockMaterial()
    }

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult {
        val key = unlockedKey.value
            ?: return RefreshVaultAndroidCredentialProtectedMaterialResult.VaultLocked

        return refreshAndroidCredentialProtectedUnlockMaterial(
            key = key,
            clearStoredMaterialOnFailure = true
        )
    }

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult = keyAccessMutex.withLock {
        changePinWithExclusiveKeyAccess(currentPin, newPin)
    }

    private suspend fun changePinWithExclusiveKeyAccess(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult {
        if (newPin.isEmpty()) {
            return ChangeVaultPinResult.InvalidNewPin
        }

        val config = dataStore.data
            .safeVaultPreferences
            .map { prefs -> prefs.toVaultStoredConfigOrNull() }
            .first()
            ?: return ChangeVaultPinResult.VaultNotConfigured

        // The Vault must already be unlocked so that the rewrapper has the
        // current key available. We still verify the current PIN against the
        // stored verifier: an unlocked Vault is not enough proof that the
        // caller knows the previous PIN.
        val currentKey = unlockedKey.value ?: return ChangeVaultPinResult.VaultLocked

        if (!pinHasher.verify(currentPin, config.pinHash)) {
            return ChangeVaultPinResult.WrongCurrentPin
        }

        val newPinHash = runCatching { pinHasher.hash(newPin) }
            .getOrElse { return ChangeVaultPinResult.InvalidNewPin }

        val newKeyParams = runCatching { keyDeriver.newParams() }
            .getOrElse { return ChangeVaultPinResult.RewrapFailed }

        val newKey: SecretKey = runCatching { keyDeriver.deriveKey(newPin, newKeyParams) }
            .getOrElse { return ChangeVaultPinResult.RewrapFailed }

        check(::noteRewrapper.isInitialized) { "Vault note maintenance is not bound" }

        // Rewrap notes BEFORE overwriting persisted verifier/params so that a
        // failure here leaves the existing PIN, key and ciphertexts coherent.
        val rewrapTransaction = try {
            noteRewrapper.rewrapAllVaultNotesWith(currentKey, newKey)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return ChangeVaultPinResult.RewrapFailed
        }

        try {
            currentCoroutineContext().ensureActive()
        } catch (error: CancellationException) {
            rollbackWithoutReplacing(rewrapTransaction, error)
            throw error
        }

        var configCommitted = false
        try {
            // Once this point-of-no-return starts, finish the atomic DataStore
            // commit and in-memory hand-off even if the caller is cancelled.
            // Cancellation before this block rolls the prepared Room/filesystem
            // transaction back; after it, the Vault is fully under the new key.
            withContext(NonCancellable) {
                dataStore.edit { prefs ->
                    prefs[PIN_ALGORITHM_KEY] = newPinHash.algorithm
                    prefs[PIN_ITERATIONS_KEY] = newPinHash.iterations
                    prefs[PIN_SALT_KEY] = newPinHash.saltBase64
                    prefs[PIN_HASH_KEY] = newPinHash.hashBase64
                    prefs[KEY_DERIVATION_ALGORITHM_KEY] = newKeyParams.algorithm
                    prefs[KEY_DERIVATION_ITERATIONS_KEY] = newKeyParams.iterations
                    prefs[KEY_DERIVATION_SALT_KEY] = newKeyParams.saltBase64
                    prefs[KEY_DERIVATION_KEY_LENGTH_BITS_KEY] = newKeyParams.keyLengthBits
                }
                configCommitted = true

                // Swap the in-memory key only after persistence is in place so
                // every subsequent Vault operation uses the new key.
                unlockedKey.value = newKey

                // Backup cleanup cannot invalidate the committed Vault. A
                // failed deletion leaves only old ciphertext, never plaintext.
                try {
                    rewrapTransaction.commit()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    NexNoteDebugLog.repositoryWarning(event = "vaultRewrapBackupCleanupFailed") {
                        "error=${error::class.java.simpleName}"
                    }
                }

                refreshAndroidCredentialProtectedUnlockMaterial(
                    key = newKey,
                    clearStoredMaterialOnFailure = true
                )
            }
        } catch (error: CancellationException) {
            if (!configCommitted) {
                rollbackWithoutReplacing(rewrapTransaction, error)
            }
            throw error
        } catch (_: Exception) {
            if (!configCommitted) {
                val rollbackSucceeded = try {
                    withContext(NonCancellable) { rewrapTransaction.rollback() }
                    true
                } catch (_: Exception) {
                    false
                }
                if (!rollbackSucceeded) {
                    unlockedKey.value = null
                }
                return ChangeVaultPinResult.RewrapFailed
            }
        }

        return ChangeVaultPinResult.Success
    }

    private suspend fun rollbackWithoutReplacing(
        transaction: VaultNoteRewrapTransaction,
        originalFailure: Throwable
    ) {
        withContext(NonCancellable) {
            try {
                transaction.rollback()
            } catch (rollbackFailure: Throwable) {
                originalFailure.addSuppressed(rollbackFailure)
            }
        }
    }

    override suspend fun resetVault(): ResetVaultResult {
        val isConfigured = dataStore.data
            .safeVaultPreferences
            .map { prefs -> prefs.toVaultStoredConfigOrNull() != null }
            .first()
        if (!isConfigured) {
            return ResetVaultResult.VaultNotConfigured
        }

        if (unlockedKey.value == null) {
            return ResetVaultResult.VaultLocked
        }

        // Hard-delete encrypted Vault notes BEFORE clearing the persisted
        // verifier/key parameters. The Vault must be unlocked before reset so
        // the destructive operation is always preceded by authentication. If
        // the wipe fails the Vault is left intact (PIN and ciphertexts
        // unchanged).
        check(::noteWiper.isInitialized) { "Vault note maintenance is not bound" }
        val wipeSucceeded = runCatchingPreservingCancellation {
            noteWiper.wipeAllVaultNotes()
        }.isSuccess
        if (!wipeSucceeded) {
            return ResetVaultResult.Failed
        }

        // Even if the DataStore edit below fails, the encrypted notes are
        // already gone, so the in-memory key has no useful payload to decrypt.
        // Discard it eagerly so no caller can still observe an UNLOCKED state
        // for a Vault that is logically being reset.
        unlockedKey.value = null

        val storeCleared = runCatchingPreservingCancellation {
            dataStore.edit { prefs ->
                prefs.remove(PIN_ALGORITHM_KEY)
                prefs.remove(PIN_ITERATIONS_KEY)
                prefs.remove(PIN_SALT_KEY)
                prefs.remove(PIN_HASH_KEY)
                prefs.remove(KEY_DERIVATION_ALGORITHM_KEY)
                prefs.remove(KEY_DERIVATION_ITERATIONS_KEY)
                prefs.remove(KEY_DERIVATION_SALT_KEY)
                prefs.remove(KEY_DERIVATION_KEY_LENGTH_BITS_KEY)
                prefs.remove(ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY)
            }
        }.isSuccess

        return if (storeCleared) ResetVaultResult.Success else ResetVaultResult.Failed
    }

    override suspend fun <T> withUnlockedVaultKey(block: suspend (SecretKey) -> T): T? {
        return keyAccessMutex.withLock {
            val key = unlockedKey.value ?: return@withLock null
            block(key)
        }
    }

    private suspend fun refreshAndroidCredentialProtectedUnlockMaterial(
        key: SecretKey,
        clearStoredMaterialOnFailure: Boolean
    ): RefreshVaultAndroidCredentialProtectedMaterialResult {
        val protector = protectUnlockMaterial
            ?: run {
                if (clearStoredMaterialOnFailure) {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                }
                return RefreshVaultAndroidCredentialProtectedMaterialResult.Failed
            }
        val material = key.encoded
            ?: run {
                if (clearStoredMaterialOnFailure) {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                }
                return RefreshVaultAndroidCredentialProtectedMaterialResult.Failed
            }

        return try {
            when (val result = protector(material)) {
                is ProtectVaultUnlockMaterialResult.Success -> {
                    dataStore.edit { prefs ->
                        prefs[ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY] =
                            result.protectedMaterial.encode()
                    }
                    RefreshVaultAndroidCredentialProtectedMaterialResult.Success
                }

                ProtectVaultUnlockMaterialResult.CredentialUnavailable -> {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                    RefreshVaultAndroidCredentialProtectedMaterialResult.CredentialUnavailable
                }

                ProtectVaultUnlockMaterialResult.KeyInvalidated -> {
                    clearStoredAndroidCredentialProtectedUnlockMaterial()
                    RefreshVaultAndroidCredentialProtectedMaterialResult.KeyInvalidated
                }

                ProtectVaultUnlockMaterialResult.AuthenticationRequired -> {
                    if (clearStoredMaterialOnFailure) {
                        clearStoredAndroidCredentialProtectedUnlockMaterial()
                    }
                    RefreshVaultAndroidCredentialProtectedMaterialResult.AuthenticationRequired
                }

                ProtectVaultUnlockMaterialResult.EmptyMaterial,
                ProtectVaultUnlockMaterialResult.Failed -> {
                    if (clearStoredMaterialOnFailure) {
                        clearStoredAndroidCredentialProtectedUnlockMaterial()
                    }
                    RefreshVaultAndroidCredentialProtectedMaterialResult.Failed
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            RefreshVaultAndroidCredentialProtectedMaterialResult.Failed
        } catch (error: Exception) {
            RefreshVaultAndroidCredentialProtectedMaterialResult.Failed
        } finally {
            material.fill(0)
        }
    }

    private suspend fun clearStoredAndroidCredentialProtectedUnlockMaterial() {
        runCatchingPreservingCancellation {
            dataStore.edit { prefs ->
                prefs.remove(ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY)
            }
        }
    }

    private suspend fun unlockWithRecoveredAndroidCredentialMaterial(
        material: ByteArray,
        config: VaultStoredConfig
    ): UnlockVaultWithAndroidCredentialResult {
        return try {
            if (!material.isSupportedAesKeyMaterial(config.keyParams.keyLengthBits)) {
                clearStoredAndroidCredentialProtectedUnlockMaterial()
                return UnlockVaultWithAndroidCredentialResult.InvalidPayload
            }

            unlockedKey.value = SecretKeySpec(material, VaultKeyDeriver.KEY_ALGORITHM)
            UnlockVaultWithAndroidCredentialResult.Success
        } finally {
            material.fill(0)
        }
    }

    private fun ByteArray.isSupportedAesKeyMaterial(expectedKeyLengthBits: Int): Boolean {
        val actualKeyLengthBits = size * BITS_PER_BYTE
        return actualKeyLengthBits == expectedKeyLengthBits &&
            actualKeyLengthBits in SUPPORTED_AES_KEY_LENGTH_BITS
    }

    private fun Preferences.toVaultStoredConfigOrNull(): VaultStoredConfig? {
        return VaultStoredConfig(
            pinHash = toVaultPinHashOrNull() ?: return null,
            keyParams = toVaultKeyDerivationParamsOrNull() ?: return null
        )
    }

    private fun Preferences.toVaultPinHashOrNull(): VaultPinHash? {
        val algorithm = this[PIN_ALGORITHM_KEY]?.takeIf { it.isNotBlank() } ?: return null
        val iterations = this[PIN_ITERATIONS_KEY]?.takeIf { it > 0 } ?: return null
        val saltBase64 = this[PIN_SALT_KEY]?.takeIf { it.isNotBlank() } ?: return null
        val hashBase64 = this[PIN_HASH_KEY]?.takeIf { it.isNotBlank() } ?: return null

        return VaultPinHash(
            algorithm = algorithm,
            iterations = iterations,
            saltBase64 = saltBase64,
            hashBase64 = hashBase64
        )
    }

    private fun Preferences.toVaultKeyDerivationParamsOrNull(): VaultKeyDerivationParams? {
        val algorithm = this[KEY_DERIVATION_ALGORITHM_KEY]?.takeIf { it.isNotBlank() } ?: return null
        val iterations = this[KEY_DERIVATION_ITERATIONS_KEY]?.takeIf { it > 0 } ?: return null
        val saltBase64 = this[KEY_DERIVATION_SALT_KEY]?.takeIf { it.isNotBlank() } ?: return null
        val keyLengthBits = this[KEY_DERIVATION_KEY_LENGTH_BITS_KEY]?.takeIf { it > 0 } ?: return null

        return VaultKeyDerivationParams(
            algorithm = algorithm,
            iterations = iterations,
            saltBase64 = saltBase64,
            keyLengthBits = keyLengthBits
        )
    }

    private data class VaultStoredConfig(
        val pinHash: VaultPinHash,
        val keyParams: VaultKeyDerivationParams
    )

    companion object {
        val PIN_ALGORITHM_KEY = stringPreferencesKey("vault_pin_algorithm")
        val PIN_ITERATIONS_KEY = intPreferencesKey("vault_pin_iterations")
        val PIN_SALT_KEY = stringPreferencesKey("vault_pin_salt")
        val PIN_HASH_KEY = stringPreferencesKey("vault_pin_hash")
        val KEY_DERIVATION_ALGORITHM_KEY = stringPreferencesKey("vault_key_derivation_algorithm")
        val KEY_DERIVATION_ITERATIONS_KEY = intPreferencesKey("vault_key_derivation_iterations")
        val KEY_DERIVATION_SALT_KEY = stringPreferencesKey("vault_key_derivation_salt")
        val KEY_DERIVATION_KEY_LENGTH_BITS_KEY = intPreferencesKey("vault_key_derivation_key_length_bits")
        val ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY =
            stringPreferencesKey("vault_android_credential_protected_unlock_material")

        private const val BITS_PER_BYTE = 8
        private val SUPPORTED_AES_KEY_LENGTH_BITS = setOf(128, 192, 256)
    }
}
