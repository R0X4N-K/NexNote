package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialPromptResult
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.usecase.ConfigureVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultAndroidCredentialAvailabilityUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithAndroidCredentialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithPinUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VaultAccessError {
    EMPTY_PIN,
    PIN_MISMATCH,
    WRONG_PIN,
    VAULT_NOT_CONFIGURED,
    ANDROID_CREDENTIAL_UNAVAILABLE,
    ANDROID_CREDENTIAL_CANCELED,
    /**
     * The Android Keystore material previously used to unlock the Vault is no
     * longer trustworthy: either the underlying Keystore key was invalidated
     * (for example after the user changed the device credentials) or the
     * stored protected payload could not be interpreted. The data layer has
     * already discarded the stale envelope, so the user must unlock again
     * with the PIN before Android screen lock unlock can be re-enabled. This
     * error is intentionally non-sensitive and never references the PIN, the
     * Vault key bytes or any Vault content.
     */
    ANDROID_CREDENTIAL_RESET_REQUIRED,
    OPERATION_FAILED
}

@Immutable
data class VaultAccessUiState(
    val vaultState: VaultState = VaultState.NOT_CONFIGURED,
    val isBusy: Boolean = false,
    val error: VaultAccessError? = null,
    val androidCredentialAvailability: VaultAndroidCredentialAvailability =
        VaultAndroidCredentialAvailability.UNAVAILABLE,
    val isAndroidCredentialUnlockEnabled: Boolean = false,
    val hasAndroidCredentialProtectedUnlockMaterial: Boolean = false,
    val androidCredentialPromptRequestId: Long = 0L,
    val isAndroidCredentialPromptPending: Boolean = false,
    val lastAndroidCredentialPromptResult: VaultAndroidCredentialPromptResult? = null
) {
    val requiresSetup: Boolean
        get() = vaultState == VaultState.NOT_CONFIGURED

    val isUnlocked: Boolean
        get() = vaultState == VaultState.UNLOCKED

    val canUseAndroidCredential: Boolean
        get() = vaultState == VaultState.LOCKED &&
            isAndroidCredentialUnlockEnabled &&
            hasAndroidCredentialProtectedUnlockMaterial &&
            androidCredentialAvailability == VaultAndroidCredentialAvailability.AVAILABLE &&
            !isBusy &&
            !isAndroidCredentialPromptPending
}

private data class VaultAccessOperationState(
    val isBusy: Boolean = false,
    val error: VaultAccessError? = null,
    val androidCredentialPromptRequestId: Long = 0L,
    val isAndroidCredentialPromptPending: Boolean = false,
    val lastAndroidCredentialPromptResult: VaultAndroidCredentialPromptResult? = null
)

class VaultAccessViewModel(
    observeVaultState: ObserveVaultStateUseCase,
    private val configureVaultPin: ConfigureVaultPinUseCase,
    private val unlockVaultWithPin: UnlockVaultWithPinUseCase,
    private val unlockVaultWithAndroidCredential: UnlockVaultWithAndroidCredentialUseCase,
    private val lockVault: LockVaultUseCase,
    getVaultAndroidCredentialAvailability: GetVaultAndroidCredentialAvailabilityUseCase,
    observeVaultAndroidCredentialUnlock: ObserveVaultAndroidCredentialUnlockUseCase,
    observeVaultAndroidCredentialProtectedMaterial:
        ObserveVaultAndroidCredentialProtectedMaterialUseCase
) : ViewModel() {

    private val androidCredentialAvailability = getVaultAndroidCredentialAvailability()
    private val androidCredentialUnlockEnabled = observeVaultAndroidCredentialUnlock()
    private val hasAndroidCredentialProtectedUnlockMaterial =
        observeVaultAndroidCredentialProtectedMaterial()
    private val operationState = MutableStateFlow(VaultAccessOperationState())

    val uiState: StateFlow<VaultAccessUiState> =
        combine(
            observeVaultState(),
            operationState,
            androidCredentialUnlockEnabled,
            hasAndroidCredentialProtectedUnlockMaterial
        ) { vaultState, operation, androidUnlockEnabled, hasProtectedMaterial ->
            VaultAccessUiState(
                vaultState = vaultState,
                isBusy = operation.isBusy,
                error = operation.error,
                androidCredentialAvailability = androidCredentialAvailability,
                isAndroidCredentialUnlockEnabled = androidUnlockEnabled,
                hasAndroidCredentialProtectedUnlockMaterial = hasProtectedMaterial,
                androidCredentialPromptRequestId = operation.androidCredentialPromptRequestId,
                isAndroidCredentialPromptPending = operation.isAndroidCredentialPromptPending,
                lastAndroidCredentialPromptResult = operation.lastAndroidCredentialPromptResult
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VaultAccessUiState()
        )

    fun configurePin(pin: CharArray, confirmation: CharArray) {
        val pinCopy = pin.copyOf()
        val confirmationCopy = confirmation.copyOf()
        pin.wipe()
        confirmation.wipe()

        if (pinCopy.isEmpty()) {
            pinCopy.wipe()
            confirmationCopy.wipe()
            setError(VaultAccessError.EMPTY_PIN)
            return
        }

        if (!pinCopy.contentEquals(confirmationCopy)) {
            pinCopy.wipe()
            confirmationCopy.wipe()
            setError(VaultAccessError.PIN_MISMATCH)
            return
        }

        confirmationCopy.wipe()
        viewModelScope.launch {
            operationState.update { it.copy(isBusy = true, error = null) }
            try {
                configureVaultPin(pinCopy)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                operationState.update {
                    it.copy(error = VaultAccessError.OPERATION_FAILED)
                }
            } finally {
                pinCopy.wipe()
                operationState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun unlockWithPin(pin: CharArray) {
        val pinCopy = pin.copyOf()
        pin.wipe()

        if (pinCopy.isEmpty()) {
            pinCopy.wipe()
            setError(VaultAccessError.EMPTY_PIN)
            return
        }

        if (uiState.value.vaultState == VaultState.NOT_CONFIGURED) {
            pinCopy.wipe()
            setError(VaultAccessError.VAULT_NOT_CONFIGURED)
            return
        }

        viewModelScope.launch {
            operationState.update { it.copy(isBusy = true, error = null) }
            try {
                val unlocked = unlockVaultWithPin(pinCopy)
                if (!unlocked) {
                    operationState.update {
                        it.copy(error = VaultAccessError.WRONG_PIN)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                operationState.update {
                    it.copy(error = VaultAccessError.OPERATION_FAILED)
                }
            } finally {
                pinCopy.wipe()
                operationState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun requestAndroidCredentialPrompt() {
        when {
            uiState.value.vaultState == VaultState.NOT_CONFIGURED -> {
                setError(VaultAccessError.VAULT_NOT_CONFIGURED)
            }
            androidCredentialAvailability != VaultAndroidCredentialAvailability.AVAILABLE -> {
                setError(VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE)
            }
            !uiState.value.isAndroidCredentialUnlockEnabled -> {
                setError(VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE)
            }
            !uiState.value.hasAndroidCredentialProtectedUnlockMaterial -> {
                setError(VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE)
            }
            uiState.value.vaultState == VaultState.UNLOCKED -> {
                clearError()
            }
            else -> {
                operationState.update {
                    it.copy(
                        error = null,
                        isAndroidCredentialPromptPending = true,
                        lastAndroidCredentialPromptResult = null,
                        androidCredentialPromptRequestId =
                            it.androidCredentialPromptRequestId + 1L
                    )
                }
            }
        }
    }

    fun onAndroidCredentialPromptResult(result: VaultAndroidCredentialPromptResult) {
        if (result == VaultAndroidCredentialPromptResult.AUTHENTICATED) {
            if (!uiState.value.isAndroidCredentialUnlockEnabled) {
                operationState.update {
                    it.copy(
                        isAndroidCredentialPromptPending = false,
                        lastAndroidCredentialPromptResult = result,
                        error = VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE
                    )
                }
                return
            }
            if (!uiState.value.hasAndroidCredentialProtectedUnlockMaterial) {
                operationState.update {
                    it.copy(
                        isAndroidCredentialPromptPending = false,
                        lastAndroidCredentialPromptResult = result,
                        error = VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE
                    )
                }
                return
            }
            unlockAfterAuthenticatedAndroidCredential()
            return
        }

        operationState.update {
            it.copy(
                isAndroidCredentialPromptPending = false,
                lastAndroidCredentialPromptResult = result,
                error = when (result) {
                    VaultAndroidCredentialPromptResult.CANCELED ->
                        VaultAccessError.ANDROID_CREDENTIAL_CANCELED
                    VaultAndroidCredentialPromptResult.UNAVAILABLE ->
                        VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE
                    VaultAndroidCredentialPromptResult.FAILED ->
                        VaultAccessError.OPERATION_FAILED
                    VaultAndroidCredentialPromptResult.AUTHENTICATED -> null
                }
            )
        }
    }

    private fun unlockAfterAuthenticatedAndroidCredential() {
        viewModelScope.launch {
            operationState.update {
                it.copy(
                    isBusy = true,
                    error = null,
                    isAndroidCredentialPromptPending = false,
                    lastAndroidCredentialPromptResult =
                        VaultAndroidCredentialPromptResult.AUTHENTICATED
                )
            }
            try {
                val result = unlockVaultWithAndroidCredential()
                operationState.update {
                    it.copy(error = result.toAccessError())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                operationState.update {
                    it.copy(error = VaultAccessError.OPERATION_FAILED)
                }
            } finally {
                operationState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun lock() {
        lockVault()
        clearError()
    }

    fun clearError() {
        operationState.update { it.copy(error = null) }
    }

    private fun setError(error: VaultAccessError) {
        operationState.update { it.copy(isBusy = false, error = error) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val preferences = app.useCases.preferences
                val vault = app.useCases.vault
                VaultAccessViewModel(
                    observeVaultState = vault.observeVaultState,
                    configureVaultPin = vault.configureVaultPin,
                    unlockVaultWithPin = vault.unlockVaultWithPin,
                    unlockVaultWithAndroidCredential =
                        vault.unlockVaultWithAndroidCredential,
                    lockVault = vault.lockVault,
                    getVaultAndroidCredentialAvailability =
                        vault.getVaultAndroidCredentialAvailability,
                    observeVaultAndroidCredentialUnlock =
                        preferences.observeVaultAndroidCredentialUnlock,
                    observeVaultAndroidCredentialProtectedMaterial =
                        vault.observeVaultAndroidCredentialProtectedMaterial
                )
            }
        }
    }
}

private fun UnlockVaultWithAndroidCredentialResult.toAccessError(): VaultAccessError? =
    when (this) {
        UnlockVaultWithAndroidCredentialResult.Success -> null
        UnlockVaultWithAndroidCredentialResult.VaultNotConfigured ->
            VaultAccessError.VAULT_NOT_CONFIGURED
        UnlockVaultWithAndroidCredentialResult.NoProtectedMaterial,
        UnlockVaultWithAndroidCredentialResult.CredentialUnavailable,
        UnlockVaultWithAndroidCredentialResult.AuthenticationRequired ->
            VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE
        UnlockVaultWithAndroidCredentialResult.KeyInvalidated,
        UnlockVaultWithAndroidCredentialResult.InvalidPayload ->
            // The repository has already removed the stale protected envelope
            // for these outcomes; surface a dedicated, non-sensitive message
            // so the user understands they need to unlock with PIN once to
            // re-enable Android screen lock unlock.
            VaultAccessError.ANDROID_CREDENTIAL_RESET_REQUIRED
        UnlockVaultWithAndroidCredentialResult.Failed ->
            VaultAccessError.OPERATION_FAILED
    }

private fun CharArray.wipe() {
    fill('\u0000')
}
