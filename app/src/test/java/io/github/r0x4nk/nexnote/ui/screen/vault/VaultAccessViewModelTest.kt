package io.github.r0x4nk.nexnote.ui.screen.vault

import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialPromptResult
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.usecase.ConfigureVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultAndroidCredentialAvailabilityUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithAndroidCredentialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithPinUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultAccessViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeVaultRepository
    private lateinit var fakeAndroidCredentialRepo: FakeVaultAndroidCredentialRepository
    private lateinit var fakePreferencesRepo: FakePreferencesRepository
    private lateinit var viewModel: VaultAccessViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeVaultRepository()
        fakeAndroidCredentialRepo = FakeVaultAndroidCredentialRepository()
        fakePreferencesRepo = FakePreferencesRepository()
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): VaultAccessViewModel {
        return VaultAccessViewModel(
            observeVaultState = ObserveVaultStateUseCase(fakeRepo),
            configureVaultPin = ConfigureVaultPinUseCase(fakeRepo),
            unlockVaultWithPin = UnlockVaultWithPinUseCase(fakeRepo),
            unlockVaultWithAndroidCredential = UnlockVaultWithAndroidCredentialUseCase(fakeRepo),
            lockVault = LockVaultUseCase(fakeRepo),
            getVaultAndroidCredentialAvailability =
                GetVaultAndroidCredentialAvailabilityUseCase(fakeAndroidCredentialRepo),
            observeVaultAndroidCredentialUnlock =
                ObserveVaultAndroidCredentialUnlockUseCase(fakePreferencesRepo),
            observeVaultAndroidCredentialProtectedMaterial =
                ObserveVaultAndroidCredentialProtectedMaterialUseCase(fakeRepo)
        )
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        block()
    }

    @Test
    fun `initial state mirrors unconfigured vault`() = runViewModelTest {
        assertEquals(VaultState.NOT_CONFIGURED, viewModel.uiState.value.vaultState)
        assertTrue(viewModel.uiState.value.requiresSetup)
    }

    @Test
    fun `initial state exposes Android credential availability`() = runViewModelTest {
        assertEquals(
            VaultAndroidCredentialAvailability.AVAILABLE,
            viewModel.uiState.value.androidCredentialAvailability
        )
    }

    @Test
    fun `configurePin rejects mismatched confirmation and clears input`() = runViewModelTest {
        val pin = "1234".toCharArray()
        val confirmation = "4321".toCharArray()

        viewModel.configurePin(pin, confirmation)
        advanceUntilIdle()

        assertEquals(VaultAccessError.PIN_MISMATCH, viewModel.uiState.value.error)
        assertEquals(0, fakeRepo.configureCalls)
        assertCleared(pin)
        assertCleared(confirmation)
    }

    @Test
    fun `configurePin configures matching pin and clears input`() = runViewModelTest {
        val pin = "1234".toCharArray()
        val confirmation = "1234".toCharArray()

        viewModel.configurePin(pin, confirmation)
        advanceUntilIdle()

        assertEquals(1, fakeRepo.configureCalls)
        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        assertNull(viewModel.uiState.value.error)
        assertCleared(pin)
        assertCleared(confirmation)
    }

    @Test
    fun `unlockWithPin rejects unconfigured vault without checking repository`() =
        runViewModelTest {
            val pin = "1234".toCharArray()

            viewModel.unlockWithPin(pin)
            advanceUntilIdle()

            assertEquals(VaultAccessError.VAULT_NOT_CONFIGURED, viewModel.uiState.value.error)
            assertEquals(0, fakeRepo.unlockCalls)
            assertCleared(pin)
        }

    @Test
    fun `unlockWithPin unlocks configured vault and clears input`() = runViewModelTest {
        fakeRepo.configureStoredPin("1234".toCharArray())
        advanceUntilIdle()
        val pin = "1234".toCharArray()

        viewModel.unlockWithPin(pin)
        advanceUntilIdle()

        assertEquals(1, fakeRepo.unlockCalls)
        assertEquals(VaultState.UNLOCKED, viewModel.uiState.value.vaultState)
        assertNull(viewModel.uiState.value.error)
        assertCleared(pin)
    }

    @Test
    fun `requestAndroidCredentialPrompt emits prompt request for locked configured vault`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            advanceUntilIdle()

            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            assertEquals(1L, viewModel.uiState.value.androidCredentialPromptRequestId)
            assertTrue(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)
            assertNull(viewModel.uiState.value.error)
            assertEquals(0, fakeRepo.unlockCalls)
            assertEquals(0, fakeRepo.androidCredentialUnlockCalls)
        }

    @Test
    fun `canUseAndroidCredential is true only when locked configured vault matches all gates`() =
        runViewModelTest {
            // Vault not configured: button must stay hidden.
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)

            // Locked vault but Android unlock preference disabled.
            fakeRepo.configureStoredPin("1234".toCharArray())
            advanceUntilIdle()
            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)

            // Preference enabled and credentials AVAILABLE are not enough.
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)

            // The button becomes available only after a successful PIN unlock
            // has created Android credential-protected unlock material.
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canUseAndroidCredential)

            // After unlock, the button must hide again on the unlocked surface.
            viewModel.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            assertEquals(VaultState.UNLOCKED, viewModel.uiState.value.vaultState)
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)
        }

    @Test
    fun `canUseAndroidCredential is false when device lock screen is not secured`() =
        runViewModelTest {
            fakeAndroidCredentialRepo.currentAvailability =
                VaultAndroidCredentialAvailability.LOCK_SCREEN_NOT_SECURED
            viewModel = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)
        }

    @Test
    fun `authenticated Android credential result unlocks vault through Android use case`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            fakeRepo.androidCredentialUnlockResult =
                UnlockVaultWithAndroidCredentialResult.Success
            advanceUntilIdle()
            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(VaultState.UNLOCKED, viewModel.uiState.value.vaultState)
            assertEquals(0, fakeRepo.unlockCalls)
            assertEquals(1, fakeRepo.androidCredentialUnlockCalls)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(
                VaultAndroidCredentialPromptResult.AUTHENTICATED,
                viewModel.uiState.value.lastAndroidCredentialPromptResult
            )
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `authenticated Android credential result reports unavailable protected material`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            fakeRepo.androidCredentialUnlockResult =
                UnlockVaultWithAndroidCredentialResult.NoProtectedMaterial
            advanceUntilIdle()
            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertEquals(1, fakeRepo.androidCredentialUnlockCalls)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `authenticated Android credential result maps KeyInvalidated to reset-required error`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            fakeRepo.androidCredentialUnlockResult =
                UnlockVaultWithAndroidCredentialResult.KeyInvalidated
            advanceUntilIdle()
            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertEquals(1, fakeRepo.androidCredentialUnlockCalls)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_RESET_REQUIRED,
                viewModel.uiState.value.error
            )
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)
        }

    @Test
    fun `authenticated Android credential result maps InvalidPayload to reset-required error`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            fakeRepo.androidCredentialUnlockResult =
                UnlockVaultWithAndroidCredentialResult.InvalidPayload
            advanceUntilIdle()
            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertEquals(1, fakeRepo.androidCredentialUnlockCalls)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_RESET_REQUIRED,
                viewModel.uiState.value.error
            )
            assertFalse(viewModel.uiState.value.canUseAndroidCredential)
        }

    @Test
    fun `successful PIN unlock after reset-required error clears the error`() = runViewModelTest {
        fakeRepo.configureStoredPin("1234".toCharArray())
        fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
        fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
        fakeRepo.androidCredentialUnlockResult =
            UnlockVaultWithAndroidCredentialResult.KeyInvalidated
        advanceUntilIdle()
        viewModel.requestAndroidCredentialPrompt()
        advanceUntilIdle()
        viewModel.onAndroidCredentialPromptResult(
            VaultAndroidCredentialPromptResult.AUTHENTICATED
        )
        advanceUntilIdle()
        assertEquals(
            VaultAccessError.ANDROID_CREDENTIAL_RESET_REQUIRED,
            viewModel.uiState.value.error
        )

        viewModel.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()

        assertEquals(VaultState.UNLOCKED, viewModel.uiState.value.vaultState)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `authenticated Android credential result is ignored when protected material is absent`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.androidCredentialUnlockResult =
                UnlockVaultWithAndroidCredentialResult.Success
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertEquals(0, fakeRepo.androidCredentialUnlockCalls)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `canceled Android credential result does not call Android unlock use case`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            fakeRepo.setHasAndroidCredentialProtectedUnlockMaterial(true)
            advanceUntilIdle()
            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.CANCELED
            )
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertEquals(0, fakeRepo.androidCredentialUnlockCalls)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_CANCELED,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `requestAndroidCredentialPrompt reports unavailable Android credential`() =
        runViewModelTest {
            fakeAndroidCredentialRepo.currentAvailability =
                VaultAndroidCredentialAvailability.LOCK_SCREEN_NOT_SECURED
            viewModel = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakePreferencesRepo.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            assertEquals(0L, viewModel.uiState.value.androidCredentialPromptRequestId)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `requestAndroidCredentialPrompt is blocked when Android unlock preference is disabled`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.canUseAndroidCredential)

            viewModel.requestAndroidCredentialPrompt()
            advanceUntilIdle()

            assertEquals(0L, viewModel.uiState.value.androidCredentialPromptRequestId)
            assertFalse(viewModel.uiState.value.isAndroidCredentialPromptPending)
            assertEquals(0, fakeRepo.androidCredentialUnlockCalls)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `authenticated Android credential result does not unlock when preference is disabled`() =
        runViewModelTest {
            fakeRepo.configureStoredPin("1234".toCharArray())
            fakeRepo.androidCredentialUnlockResult =
                UnlockVaultWithAndroidCredentialResult.Success
            advanceUntilIdle()

            viewModel.onAndroidCredentialPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
            assertEquals(0, fakeRepo.androidCredentialUnlockCalls)
            assertEquals(
                VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `unlockWithPin reports wrong pin and keeps vault locked`() = runViewModelTest {
        fakeRepo.configureStoredPin("1234".toCharArray())
        advanceUntilIdle()
        val pin = "0000".toCharArray()

        viewModel.unlockWithPin(pin)
        advanceUntilIdle()

        assertEquals(1, fakeRepo.unlockCalls)
        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        assertEquals(VaultAccessError.WRONG_PIN, viewModel.uiState.value.error)
        assertCleared(pin)
    }

    @Test
    fun `lock delegates and clears access error`() = runViewModelTest {
        fakeRepo.configureStoredPin("1234".toCharArray())
        advanceUntilIdle()
        viewModel.unlockWithPin("0000".toCharArray())
        advanceUntilIdle()
        assertEquals(VaultAccessError.WRONG_PIN, viewModel.uiState.value.error)

        viewModel.lock()
        advanceUntilIdle()

        assertEquals(1, fakeRepo.lockCalls)
        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        assertNull(viewModel.uiState.value.error)
    }

    private fun assertCleared(pin: CharArray) {
        assertTrue(pin.all { it == '\u0000' })
    }
}

private class FakeVaultRepository : VaultRepository {
    private val vaultState = MutableStateFlow(VaultState.NOT_CONFIGURED)
    private val hasProtectedMaterial = MutableStateFlow(false)
    private var storedPin: CharArray? = null

    var configureCalls = 0
        private set
    var unlockCalls = 0
        private set
    var androidCredentialUnlockCalls = 0
        private set
    var lockCalls = 0
        private set
    var androidCredentialUnlockResult: UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override val state: Flow<VaultState> = vaultState
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        hasProtectedMaterial

    override suspend fun configurePin(pin: CharArray) {
        configureCalls += 1
        configureStoredPin(pin)
    }

    override suspend fun unlockWithPin(pin: CharArray): Boolean {
        unlockCalls += 1
        val unlocked = storedPin?.contentEquals(pin) == true
        vaultState.value = if (unlocked) VaultState.UNLOCKED else VaultState.LOCKED
        if (unlocked) {
            hasProtectedMaterial.value = true
        }
        return unlocked
    }

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult {
        androidCredentialUnlockCalls += 1
        if (androidCredentialUnlockResult == UnlockVaultWithAndroidCredentialResult.Success) {
            vaultState.value = VaultState.UNLOCKED
        } else if (
            androidCredentialUnlockResult ==
            UnlockVaultWithAndroidCredentialResult.KeyInvalidated ||
            androidCredentialUnlockResult ==
            UnlockVaultWithAndroidCredentialResult.InvalidPayload ||
            androidCredentialUnlockResult ==
            UnlockVaultWithAndroidCredentialResult.CredentialUnavailable ||
            androidCredentialUnlockResult ==
            UnlockVaultWithAndroidCredentialResult.NoProtectedMaterial
        ) {
            hasProtectedMaterial.value = false
        }
        return androidCredentialUnlockResult
    }

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult =
        RefreshVaultAndroidCredentialProtectedMaterialResult.Failed

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() {
        hasProtectedMaterial.value = false
    }

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult = ChangeVaultPinResult.RewrapFailed

    override suspend fun resetVault(): ResetVaultResult = ResetVaultResult.Failed

    override fun lock() {
        lockCalls += 1
        if (storedPin == null) {
            vaultState.value = VaultState.NOT_CONFIGURED
        } else {
            vaultState.value = VaultState.LOCKED
        }
    }

    fun configureStoredPin(pin: CharArray) {
        storedPin?.fill('\u0000')
        storedPin = pin.copyOf()
        hasProtectedMaterial.value = false
        vaultState.value = VaultState.LOCKED
    }

    fun setHasAndroidCredentialProtectedUnlockMaterial(value: Boolean) {
        hasProtectedMaterial.value = value
    }
}

private class FakeVaultAndroidCredentialRepository : VaultAndroidCredentialRepository {
    var currentAvailability: VaultAndroidCredentialAvailability =
        VaultAndroidCredentialAvailability.AVAILABLE

    override fun getAvailability(): VaultAndroidCredentialAvailability = currentAvailability
}

private class FakePreferencesRepository : IUserPreferencesRepository {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _fontScale = MutableStateFlow(FontScale.NORMAL)
    private val _timezoneId = MutableStateFlow("")
    private val _isLeftHanded = MutableStateFlow(false)
    private val _accentColor = MutableStateFlow(AccentColor.VIOLET)
    private val _noteCardStyle = MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)
    private val _protectVaultRecentPreviews = MutableStateFlow(true)
    private val _lockVaultOnBackground = MutableStateFlow(true)
    private val _vaultAutoLockTimeout =
        MutableStateFlow(VaultAutoLockTimeout.IMMEDIATELY)
    private val _unlockVaultWithAndroidCredential = MutableStateFlow(false)

    override val themeMode: Flow<ThemeMode> = _themeMode
    override val fontScale: Flow<FontScale> = _fontScale
    override val timezoneId: Flow<String> = _timezoneId
    override val isLeftHanded: Flow<Boolean> = _isLeftHanded
    override val accentColor: Flow<AccentColor> = _accentColor
    override val noteCardStyle: Flow<NoteCardStyle> = _noteCardStyle
    override val protectVaultRecentPreviews: Flow<Boolean> = _protectVaultRecentPreviews
    override val lockVaultOnBackground: Flow<Boolean> = _lockVaultOnBackground
    override val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout> = _vaultAutoLockTimeout
    override val unlockVaultWithAndroidCredential: Flow<Boolean> =
        _unlockVaultWithAndroidCredential

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    override suspend fun setFontScale(scale: FontScale) {
        _fontScale.value = scale
    }

    override suspend fun setTimezoneId(id: String) {
        _timezoneId.value = id
    }

    override suspend fun setLeftHanded(value: Boolean) {
        _isLeftHanded.value = value
    }

    override suspend fun setAccentColor(color: AccentColor) {
        _accentColor.value = color
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        _noteCardStyle.value = style
    }

    override suspend fun setProtectVaultRecentPreviews(value: Boolean) {
        _protectVaultRecentPreviews.value = value
    }

    override suspend fun setLockVaultOnBackground(value: Boolean) {
        _lockVaultOnBackground.value = value
    }

    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) {
        _vaultAutoLockTimeout.value = timeout
    }

    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) {
        _unlockVaultWithAndroidCredential.value = value
    }
}
