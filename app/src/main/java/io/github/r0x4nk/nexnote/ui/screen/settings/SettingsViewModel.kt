package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.di.requireAppDependencies
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialPromptResult
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.usecase.ChangeVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ClearVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTableLayoutModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultRecentPreviewsProtectionUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RefreshVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ResetVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetTableLayoutModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultRecentPreviewsProtectionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val themeMode:          ThemeMode     = ThemeMode.SYSTEM,
    val fontScale:          FontScale     = FontScale.NORMAL,
    val timezoneId:         String        = "",
    val availableTimezones: List<String>  = emptyList(),
    val accentColor:        AccentColor   = AccentColor.VIOLET,
    val noteCardStyle:      NoteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW,
    val tableLayoutMode:    TableLayoutMode = TableLayoutMode.FIT_SCREEN,
    val vaultState:         VaultState    = VaultState.NOT_CONFIGURED,
    val protectVaultRecentPreviews: Boolean = true,
    val lockVaultOnBackground: Boolean = true,
    val vaultAutoLockTimeout: VaultAutoLockTimeout = VaultAutoLockTimeout.IMMEDIATELY,
    val unlockVaultWithAndroidCredential: Boolean = false
) {
    val canChangeVaultPin: Boolean
        get() = vaultState == VaultState.UNLOCKED

    val canConfigureAndroidCredentialUnlock: Boolean
        get() = vaultState == VaultState.UNLOCKED ||
            (vaultState == VaultState.LOCKED && unlockVaultWithAndroidCredential)
}

enum class SettingsVaultPinChangeError {
    EMPTY_CURRENT_PIN,
    EMPTY_NEW_PIN,
    PIN_MISMATCH,
    VAULT_NOT_CONFIGURED,
    VAULT_LOCKED,
    WRONG_CURRENT_PIN,
    OPERATION_FAILED
}

@Immutable
data class SettingsVaultPinChangeUiState(
    val isBusy: Boolean = false,
    val error: SettingsVaultPinChangeError? = null,
    val isSuccessful: Boolean = false,
    val androidCredentialRefreshPromptRequestId: Long = 0L,
    val isAndroidCredentialRefreshPromptPending: Boolean = false
)

enum class SettingsVaultResetError {
    VAULT_NOT_CONFIGURED,
    VAULT_LOCKED,
    OPERATION_FAILED
}

@Immutable
data class SettingsVaultResetUiState(
    val isConfirmationVisible: Boolean = false,
    val isBusy: Boolean = false,
    val error: SettingsVaultResetError? = null,
    val isSuccessful: Boolean = false
)

private enum class SettingsAndroidCredentialRefreshReason {
    ENABLE_UNLOCK,
    PIN_CHANGE
}

class SettingsViewModel(
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val observeFontScale: ObserveFontScaleUseCase,
    private val observeTimezoneId: ObserveTimezoneIdUseCase,
    private val observeAccentColor: ObserveAccentColorUseCase,
    private val observeNoteCardStyle: ObserveNoteCardStyleUseCase,
    private val observeTableLayoutMode: ObserveTableLayoutModeUseCase,
    private val observeVaultState: ObserveVaultStateUseCase,
    private val observeVaultRecentPreviewsProtection: ObserveVaultRecentPreviewsProtectionUseCase,
    private val observeVaultLockOnBackground: ObserveVaultLockOnBackgroundUseCase,
    private val observeVaultAutoLockTimeout: ObserveVaultAutoLockTimeoutUseCase,
    private val observeVaultAndroidCredentialUnlock: ObserveVaultAndroidCredentialUnlockUseCase,
    private val lockVaultUseCase: LockVaultUseCase,
    private val changeVaultPinUseCase: ChangeVaultPinUseCase,
    private val resetVaultUseCase: ResetVaultUseCase,
    private val refreshVaultAndroidCredentialProtectedMaterialUseCase:
        RefreshVaultAndroidCredentialProtectedMaterialUseCase,
    private val clearVaultAndroidCredentialProtectedMaterialUseCase:
        ClearVaultAndroidCredentialProtectedMaterialUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setFontScaleUseCase: SetFontScaleUseCase,
    private val setTimezoneIdUseCase: SetTimezoneIdUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    private val setNoteCardStyleUseCase: SetNoteCardStyleUseCase,
    private val setTableLayoutModeUseCase: SetTableLayoutModeUseCase,
    private val setVaultRecentPreviewsProtectionUseCase: SetVaultRecentPreviewsProtectionUseCase,
    private val setVaultLockOnBackgroundUseCase: SetVaultLockOnBackgroundUseCase,
    private val setVaultAutoLockTimeoutUseCase: SetVaultAutoLockTimeoutUseCase,
    private val setVaultAndroidCredentialUnlockUseCase: SetVaultAndroidCredentialUnlockUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = buildSettingsUiStateFlow(
        flows = SettingsUiStateFlows(
            themeMode = observeThemeMode(),
            fontScale = observeFontScale(),
            timezoneId = observeTimezoneId(),
            accentColor = observeAccentColor(),
            noteCardStyle = observeNoteCardStyle(),
            tableLayoutMode = observeTableLayoutMode(),
            vaultState = observeVaultState(),
            protectVaultRecentPreviews = observeVaultRecentPreviewsProtection(),
            lockVaultOnBackground = observeVaultLockOnBackground(),
            vaultAutoLockTimeout = observeVaultAutoLockTimeout(),
            unlockVaultWithAndroidCredential = observeVaultAndroidCredentialUnlock()
        ),
        scope = viewModelScope
    )

    private val _vaultPinChangeState = MutableStateFlow(SettingsVaultPinChangeUiState())
    val vaultPinChangeState: StateFlow<SettingsVaultPinChangeUiState> =
        _vaultPinChangeState.asStateFlow()

    private val _vaultResetState = MutableStateFlow(SettingsVaultResetUiState())
    val vaultResetState: StateFlow<SettingsVaultResetUiState> =
        _vaultResetState.asStateFlow()

    private var pendingAndroidCredentialRefreshReason:
        SettingsAndroidCredentialRefreshReason? = null

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { setThemeModeUseCase(mode) }
    }

    fun setFontScale(scale: FontScale) {
        viewModelScope.launch { setFontScaleUseCase(scale) }
    }

    fun setTimezoneId(id: String) {
        viewModelScope.launch { setTimezoneIdUseCase(id) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { setAccentColorUseCase(color) }
    }

    fun setNoteCardStyle(style: NoteCardStyle) {
        viewModelScope.launch { setNoteCardStyleUseCase(style) }
    }

    fun setTableLayoutMode(mode: TableLayoutMode) {
        viewModelScope.launch { setTableLayoutModeUseCase(mode) }
    }

    fun setProtectVaultRecentPreviews(value: Boolean) {
        viewModelScope.launch { setVaultRecentPreviewsProtectionUseCase(value) }
    }

    fun setLockVaultOnBackground(value: Boolean) {
        viewModelScope.launch { setVaultLockOnBackgroundUseCase(value) }
    }

    fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) {
        viewModelScope.launch { setVaultAutoLockTimeoutUseCase(timeout) }
    }

    fun setUnlockVaultWithAndroidCredential(value: Boolean) {
        val currentState = uiState.value
        if (!currentState.canConfigureAndroidCredentialUnlock) return
        if (value && currentState.vaultState != VaultState.UNLOCKED) return
        viewModelScope.launch {
            setVaultAndroidCredentialUnlockUseCase(value)
            if (value) {
                pendingAndroidCredentialRefreshReason =
                    SettingsAndroidCredentialRefreshReason.ENABLE_UNLOCK
                _vaultPinChangeState.update {
                    it.copy(
                        error = null,
                        androidCredentialRefreshPromptRequestId =
                            it.androidCredentialRefreshPromptRequestId + 1L,
                        isAndroidCredentialRefreshPromptPending = true
                    )
                }
            } else {
                pendingAndroidCredentialRefreshReason = null
                clearVaultAndroidCredentialProtectedMaterialUseCase()
                _vaultPinChangeState.update {
                    it.copy(isAndroidCredentialRefreshPromptPending = false)
                }
            }
        }
    }

    fun lockVault() {
        if (uiState.value.vaultState == VaultState.UNLOCKED) {
            lockVaultUseCase()
        }
    }

    fun changeVaultPin(
        currentPin: CharArray,
        newPin: CharArray,
        confirmation: CharArray
    ) {
        val currentPinCopy = currentPin.copyOf()
        val newPinCopy = newPin.copyOf()
        val confirmationCopy = confirmation.copyOf()
        currentPin.wipe()
        newPin.wipe()
        confirmation.wipe()

        when {
            currentPinCopy.isEmpty() -> {
                wipeVaultPinChangeInputs(currentPinCopy, newPinCopy, confirmationCopy)
                setVaultPinChangeError(SettingsVaultPinChangeError.EMPTY_CURRENT_PIN)
                return
            }

            newPinCopy.isEmpty() -> {
                wipeVaultPinChangeInputs(currentPinCopy, newPinCopy, confirmationCopy)
                setVaultPinChangeError(SettingsVaultPinChangeError.EMPTY_NEW_PIN)
                return
            }

            !newPinCopy.contentEquals(confirmationCopy) -> {
                wipeVaultPinChangeInputs(currentPinCopy, newPinCopy, confirmationCopy)
                setVaultPinChangeError(SettingsVaultPinChangeError.PIN_MISMATCH)
                return
            }

            uiState.value.vaultState == VaultState.NOT_CONFIGURED -> {
                wipeVaultPinChangeInputs(currentPinCopy, newPinCopy, confirmationCopy)
                setVaultPinChangeError(SettingsVaultPinChangeError.VAULT_NOT_CONFIGURED)
                return
            }

            uiState.value.vaultState != VaultState.UNLOCKED -> {
                wipeVaultPinChangeInputs(currentPinCopy, newPinCopy, confirmationCopy)
                setVaultPinChangeError(SettingsVaultPinChangeError.VAULT_LOCKED)
                return
            }
        }

        confirmationCopy.wipe()
        viewModelScope.launch {
            _vaultPinChangeState.update {
                it.copy(isBusy = true, error = null, isSuccessful = false)
            }
            try {
                when (changeVaultPinUseCase(currentPinCopy, newPinCopy)) {
                    ChangeVaultPinResult.Success -> {
                        val shouldRefreshAndroidMaterial =
                            uiState.value.unlockVaultWithAndroidCredential
                        pendingAndroidCredentialRefreshReason =
                            if (shouldRefreshAndroidMaterial) {
                                SettingsAndroidCredentialRefreshReason.PIN_CHANGE
                            } else {
                                null
                            }
                        _vaultPinChangeState.update {
                            it.copy(
                                error = null,
                                isSuccessful = true,
                                androidCredentialRefreshPromptRequestId =
                                    if (shouldRefreshAndroidMaterial) {
                                        it.androidCredentialRefreshPromptRequestId + 1L
                                    } else {
                                        it.androidCredentialRefreshPromptRequestId
                                    },
                                isAndroidCredentialRefreshPromptPending =
                                    shouldRefreshAndroidMaterial
                            )
                        }
                    }

                    ChangeVaultPinResult.VaultNotConfigured -> {
                        setVaultPinChangeError(SettingsVaultPinChangeError.VAULT_NOT_CONFIGURED)
                    }

                    ChangeVaultPinResult.VaultLocked -> {
                        setVaultPinChangeError(SettingsVaultPinChangeError.VAULT_LOCKED)
                    }

                    ChangeVaultPinResult.WrongCurrentPin -> {
                        setVaultPinChangeError(SettingsVaultPinChangeError.WRONG_CURRENT_PIN)
                    }

                    ChangeVaultPinResult.InvalidNewPin -> {
                        setVaultPinChangeError(SettingsVaultPinChangeError.EMPTY_NEW_PIN)
                    }

                    ChangeVaultPinResult.RewrapFailed -> {
                        setVaultPinChangeError(SettingsVaultPinChangeError.OPERATION_FAILED)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                setVaultPinChangeError(SettingsVaultPinChangeError.OPERATION_FAILED)
            } finally {
                currentPinCopy.wipe()
                newPinCopy.wipe()
                _vaultPinChangeState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun onAndroidCredentialRefreshPromptResult(result: VaultAndroidCredentialPromptResult) {
        if (!_vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending) return

        val refreshReason = pendingAndroidCredentialRefreshReason
        if (result != VaultAndroidCredentialPromptResult.AUTHENTICATED) {
            pendingAndroidCredentialRefreshReason = null
            _vaultPinChangeState.update {
                it.copy(isAndroidCredentialRefreshPromptPending = false)
            }
            if (refreshReason == SettingsAndroidCredentialRefreshReason.ENABLE_UNLOCK) {
                viewModelScope.launch {
                    setVaultAndroidCredentialUnlockUseCase(false)
                    clearVaultAndroidCredentialProtectedMaterialUseCase()
                }
            }
            return
        }

        viewModelScope.launch {
            val refreshResult = try {
                refreshVaultAndroidCredentialProtectedMaterialUseCase()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                RefreshVaultAndroidCredentialProtectedMaterialResult.Failed
            }
            if (
                refreshReason == SettingsAndroidCredentialRefreshReason.ENABLE_UNLOCK &&
                refreshResult != RefreshVaultAndroidCredentialProtectedMaterialResult.Success
            ) {
                setVaultAndroidCredentialUnlockUseCase(false)
                clearVaultAndroidCredentialProtectedMaterialUseCase()
            }
            pendingAndroidCredentialRefreshReason = null
            _vaultPinChangeState.update {
                it.copy(
                    isAndroidCredentialRefreshPromptPending = false,
                    error = when (refreshResult) {
                        RefreshVaultAndroidCredentialProtectedMaterialResult.Success -> null
                        RefreshVaultAndroidCredentialProtectedMaterialResult.VaultLocked,
                        RefreshVaultAndroidCredentialProtectedMaterialResult.CredentialUnavailable,
                        RefreshVaultAndroidCredentialProtectedMaterialResult.AuthenticationRequired,
                        RefreshVaultAndroidCredentialProtectedMaterialResult.KeyInvalidated,
                        RefreshVaultAndroidCredentialProtectedMaterialResult.Failed -> null
                    }
                )
            }
        }
    }

    fun clearVaultPinChangeFeedback() {
        _vaultPinChangeState.update {
            it.copy(error = null, isSuccessful = false)
        }
    }

    private fun setVaultPinChangeError(error: SettingsVaultPinChangeError) {
        _vaultPinChangeState.update {
            it.copy(isBusy = false, error = error, isSuccessful = false)
        }
    }

    /**
     * Request a Vault reset. The actual destructive operation is deferred to
     * [confirmVaultReset] so the UI can show an explicit, non-sensitive
     * confirmation step. The request is ignored while another reset is busy
     * or when no Vault is configured or the Vault is locked. Reset is
     * destructive, so it is only offered after the user has authenticated and
     * the Vault is currently unlocked.
     */
    fun requestVaultReset() {
        if (_vaultResetState.value.isBusy) return
        when (uiState.value.vaultState) {
            VaultState.NOT_CONFIGURED -> {
                _vaultResetState.update {
                    it.copy(
                        isConfirmationVisible = false,
                        error = SettingsVaultResetError.VAULT_NOT_CONFIGURED,
                        isSuccessful = false
                    )
                }
                return
            }

            VaultState.LOCKED -> {
                setVaultResetLockedError()
                return
            }

            VaultState.UNLOCKED -> Unit
        }
        _vaultResetState.update {
            it.copy(
                isConfirmationVisible = true,
                error = null,
                isSuccessful = false
            )
        }
    }

    private fun setVaultResetLockedError() {
        _vaultResetState.update {
            it.copy(
                isConfirmationVisible = false,
                isBusy = false,
                error = SettingsVaultResetError.VAULT_LOCKED,
                isSuccessful = false
            )
        }
    }

    /**
     * Dismiss the reset confirmation without performing any destructive
     * operation. Leaves persisted Vault state and user preferences intact.
     */
    fun cancelVaultReset() {
        if (_vaultResetState.value.isBusy) return
        _vaultResetState.update {
            it.copy(isConfirmationVisible = false)
        }
    }

    /**
     * Execute the Vault reset after explicit confirmation.
     *
     * Coordination notes:
     * - The Android screen-lock unlock toggle ([unlockVaultWithAndroidCredential])
     *   is set to `false` before invoking [resetVaultUseCase] so the user
     *   preference does not survive a successful wipe. The Keystore envelope
     *   is wiped by the repository as part of the reset; we still avoid
     *   leaving an inconsistent UI toggle around. If the reset itself fails,
     *   we deliberately do not re-enable the preference: the safer default
     *   after a failed reset is to require an explicit user opt-in again.
     * - This method never accepts, persists or logs any PIN or key material:
     *   the reset use case maps everything into a non-sensitive sealed
     *   result.
     */
    fun confirmVaultReset() {
        if (_vaultResetState.value.isBusy) return
        if (!_vaultResetState.value.isConfirmationVisible) return
        if (uiState.value.vaultState != VaultState.UNLOCKED) {
            setVaultResetLockedError()
            return
        }

        _vaultResetState.update {
            it.copy(
                isConfirmationVisible = false,
                isBusy = true,
                error = null,
                isSuccessful = false
            )
        }

        viewModelScope.launch {
            try {
                if (uiState.value.vaultState != VaultState.UNLOCKED) {
                    setVaultResetLockedError()
                    return@launch
                }
                if (uiState.value.unlockVaultWithAndroidCredential) {
                    setVaultAndroidCredentialUnlockUseCase(false)
                }
                val result = resetVaultUseCase()
                _vaultResetState.update {
                    when (result) {
                        ResetVaultResult.Success ->
                            it.copy(
                                isBusy = false,
                                error = null,
                                isSuccessful = true
                            )

                        ResetVaultResult.VaultNotConfigured ->
                            it.copy(
                                isBusy = false,
                                error = SettingsVaultResetError.VAULT_NOT_CONFIGURED,
                                isSuccessful = false
                            )

                        ResetVaultResult.VaultLocked ->
                            it.copy(
                                isBusy = false,
                                error = SettingsVaultResetError.VAULT_LOCKED,
                                isSuccessful = false
                            )

                        ResetVaultResult.Failed ->
                            it.copy(
                                isBusy = false,
                                error = SettingsVaultResetError.OPERATION_FAILED,
                                isSuccessful = false
                            )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _vaultResetState.update {
                    it.copy(
                        isBusy = false,
                        error = SettingsVaultResetError.OPERATION_FAILED,
                        isSuccessful = false
                    )
                }
            }
        }
    }

    /** Clear the transient success/error feedback for the reset flow. */
    fun clearVaultResetFeedback() {
        _vaultResetState.update {
            it.copy(error = null, isSuccessful = false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = requireAppDependencies()
                val preferences = app.useCases.preferences
                val vault = app.useCases.vault
                SettingsViewModel(
                    observeThemeMode = preferences.observeThemeMode,
                    observeFontScale = preferences.observeFontScale,
                    observeTimezoneId = preferences.observeTimezoneId,
                    observeAccentColor = preferences.observeAccentColor,
                    observeNoteCardStyle = preferences.observeNoteCardStyle,
                    observeTableLayoutMode = preferences.observeTableLayoutMode,
                    observeVaultState = vault.observeVaultState,
                    observeVaultRecentPreviewsProtection =
                        preferences.observeVaultRecentPreviewsProtection,
                    observeVaultLockOnBackground = preferences.observeVaultLockOnBackground,
                    observeVaultAutoLockTimeout = preferences.observeVaultAutoLockTimeout,
                    observeVaultAndroidCredentialUnlock =
                        preferences.observeVaultAndroidCredentialUnlock,
                    lockVaultUseCase = vault.lockVault,
                    changeVaultPinUseCase = vault.changeVaultPin,
                    resetVaultUseCase = vault.resetVault,
                    refreshVaultAndroidCredentialProtectedMaterialUseCase =
                        vault.refreshVaultAndroidCredentialProtectedMaterial,
                    clearVaultAndroidCredentialProtectedMaterialUseCase =
                        vault.clearVaultAndroidCredentialProtectedMaterial,
                    setThemeModeUseCase = preferences.setThemeMode,
                    setFontScaleUseCase = preferences.setFontScale,
                    setTimezoneIdUseCase = preferences.setTimezoneId,
                    setAccentColorUseCase = preferences.setAccentColor,
                    setNoteCardStyleUseCase = preferences.setNoteCardStyle,
                    setTableLayoutModeUseCase = preferences.setTableLayoutMode,
                    setVaultRecentPreviewsProtectionUseCase =
                        preferences.setVaultRecentPreviewsProtection,
                    setVaultLockOnBackgroundUseCase = preferences.setVaultLockOnBackground,
                    setVaultAutoLockTimeoutUseCase = preferences.setVaultAutoLockTimeout,
                    setVaultAndroidCredentialUnlockUseCase =
                        preferences.setVaultAndroidCredentialUnlock
                )
            }
        }
    }
}

private fun wipeVaultPinChangeInputs(vararg inputs: CharArray) {
    inputs.forEach { it.wipe() }
}

private fun CharArray.wipe() {
    fill('\u0000')
}
