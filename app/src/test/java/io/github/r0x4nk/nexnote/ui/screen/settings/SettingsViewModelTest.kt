package io.github.r0x4nk.nexnote.ui.screen.settings

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialPromptResult
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteStatisticsRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNormalNoteCountUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllVaultNoteCountUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteStatisticsIndexStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTableLayoutModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultRecentPreviewsProtectionUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ChangeVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ClearVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteAllStoredNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RefreshVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ResetVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RebuildNoteStatisticsIndexUseCase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakePreferencesRepository
    private lateinit var fakeVaultRepo: FakeVaultRepository
    private lateinit var fakeStoredNoteRepo: FakeStoredNoteRepository
    private lateinit var fakeStoredVaultNoteRepo: FakeStoredVaultNoteRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo  = FakePreferencesRepository()
        fakeVaultRepo = FakeVaultRepository()
        fakeStoredNoteRepo = FakeStoredNoteRepository()
        fakeStoredVaultNoteRepo = FakeStoredVaultNoteRepository()
        val statisticsRepository = FakeNoteStatisticsRepository()
        viewModel = SettingsViewModel(
            observeThemeMode = ObserveThemeModeUseCase(fakeRepo),
            observeFontScale = ObserveFontScaleUseCase(fakeRepo),
            observeTimezoneId = ObserveTimezoneIdUseCase(fakeRepo),
            observeAccentColor = ObserveAccentColorUseCase(fakeRepo),
            observeNoteCardStyle = ObserveNoteCardStyleUseCase(fakeRepo),
            observeTableLayoutMode = ObserveTableLayoutModeUseCase(fakeRepo),
            observeVaultState = ObserveVaultStateUseCase(fakeVaultRepo),
            observeVaultRecentPreviewsProtection =
                ObserveVaultRecentPreviewsProtectionUseCase(fakeRepo),
            observeVaultLockOnBackground = ObserveVaultLockOnBackgroundUseCase(fakeRepo),
            observeVaultAutoLockTimeout = ObserveVaultAutoLockTimeoutUseCase(fakeRepo),
            observeVaultAndroidCredentialUnlock =
                ObserveVaultAndroidCredentialUnlockUseCase(fakeRepo),
            observeStatisticsIndexState =
                ObserveNoteStatisticsIndexStateUseCase(statisticsRepository),
            rebuildStatisticsIndexUseCase =
                RebuildNoteStatisticsIndexUseCase(statisticsRepository),
            lockVaultUseCase = LockVaultUseCase(fakeVaultRepo),
            changeVaultPinUseCase = ChangeVaultPinUseCase(fakeVaultRepo),
            resetVaultUseCase = ResetVaultUseCase(fakeVaultRepo),
            observeAllNormalNoteCount =
                ObserveAllNormalNoteCountUseCase(fakeStoredNoteRepo),
            observeAllVaultNoteCount =
                ObserveAllVaultNoteCountUseCase(fakeStoredVaultNoteRepo),
            unlockVaultWithPinUseCase = UnlockVaultWithPinUseCase(fakeVaultRepo),
            deleteAllStoredNotesUseCase = DeleteAllStoredNotesUseCase(
                fakeStoredNoteRepo,
                fakeStoredVaultNoteRepo
            ),
            refreshVaultAndroidCredentialProtectedMaterialUseCase =
                RefreshVaultAndroidCredentialProtectedMaterialUseCase(fakeVaultRepo),
            clearVaultAndroidCredentialProtectedMaterialUseCase =
                ClearVaultAndroidCredentialProtectedMaterialUseCase(fakeVaultRepo),
            setThemeModeUseCase = SetThemeModeUseCase(fakeRepo),
            setFontScaleUseCase = SetFontScaleUseCase(fakeRepo),
            setTimezoneIdUseCase = SetTimezoneIdUseCase(fakeRepo),
            setAccentColorUseCase = SetAccentColorUseCase(fakeRepo),
            setNoteCardStyleUseCase = SetNoteCardStyleUseCase(fakeRepo),
            setTableLayoutModeUseCase = SetTableLayoutModeUseCase(fakeRepo),
            setVaultRecentPreviewsProtectionUseCase =
                SetVaultRecentPreviewsProtectionUseCase(fakeRepo),
            setVaultLockOnBackgroundUseCase = SetVaultLockOnBackgroundUseCase(fakeRepo),
            setVaultAutoLockTimeoutUseCase = SetVaultAutoLockTimeoutUseCase(fakeRepo),
            setVaultAndroidCredentialUnlockUseCase =
                SetVaultAndroidCredentialUnlockUseCase(fakeRepo)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultPinChangeState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultResetState.collect {}
        }
        block()
    }

    // ── ThemeMode ─────────────────────────────────────────────────────────────

    @Test
    fun `setThemeMode DARK is reflected in uiState`() = runViewModelTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `setThemeMode LIGHT is reflected in uiState`() = runViewModelTest {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `setThemeMode TRUE_DARK is reflected in uiState`() = runViewModelTest {
        viewModel.setThemeMode(ThemeMode.TRUE_DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.TRUE_DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `setThemeMode delegates to repository`() = runViewModelTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, fakeRepo.lastThemeMode)
    }

    // ── FontScale ─────────────────────────────────────────────────────────────

    @Test
    fun `setFontScale LARGE is reflected in uiState`() = runViewModelTest {
        viewModel.setFontScale(FontScale.LARGE)
        advanceUntilIdle()
        assertEquals(FontScale.LARGE, viewModel.uiState.value.fontScale)
    }

    @Test
    fun `setFontScale delegates to repository`() = runViewModelTest {
        viewModel.setFontScale(FontScale.SMALL)
        advanceUntilIdle()
        assertEquals(FontScale.SMALL, fakeRepo.lastFontScale)
    }

    // ── TimezoneId ────────────────────────────────────────────────────────────

    @Test
    fun `setTimezoneId is reflected in uiState`() = runViewModelTest {
        viewModel.setTimezoneId("Europe/Rome")
        advanceUntilIdle()
        assertEquals("Europe/Rome", viewModel.uiState.value.timezoneId)
    }

    @Test
    fun `setTimezoneId delegates to repository`() = runViewModelTest {
        viewModel.setTimezoneId("America/New_York")
        advanceUntilIdle()
        assertEquals("America/New_York", fakeRepo.lastTimezoneId)
    }

    // ── AccentColor ───────────────────────────────────────────────────────────

    @Test
    fun `setAccentColor BLUE is reflected in uiState`() = runViewModelTest {
        viewModel.setAccentColor(AccentColor.BLUE)
        advanceUntilIdle()
        assertEquals(AccentColor.BLUE, viewModel.uiState.value.accentColor)
    }

    @Test
    fun `setAccentColor delegates to repository`() = runViewModelTest {
        viewModel.setAccentColor(AccentColor.TEAL)
        advanceUntilIdle()
        assertEquals(AccentColor.TEAL, fakeRepo.lastAccentColor)
    }

    // ── NoteCardStyle ─────────────────────────────────────────────────────────

    @Test
    fun `setNoteCardStyle TITLE_ONLY is reflected in uiState`() = runViewModelTest {
        viewModel.setNoteCardStyle(NoteCardStyle.TITLE_ONLY)
        advanceUntilIdle()
        assertEquals(NoteCardStyle.TITLE_ONLY, viewModel.uiState.value.noteCardStyle)
    }

    @Test
    fun `setNoteCardStyle delegates to repository`() = runViewModelTest {
        viewModel.setNoteCardStyle(NoteCardStyle.TITLE_DATE)
        advanceUntilIdle()
        assertEquals(NoteCardStyle.TITLE_DATE, fakeRepo.lastNoteCardStyle)
    }

    @Test
    fun `setTableLayoutMode HORIZONTAL_SCROLL is reflected in uiState`() = runViewModelTest {
        viewModel.setTableLayoutMode(TableLayoutMode.HORIZONTAL_SCROLL)
        advanceUntilIdle()
        assertEquals(TableLayoutMode.HORIZONTAL_SCROLL, viewModel.uiState.value.tableLayoutMode)
    }

    @Test
    fun `setTableLayoutMode delegates to repository`() = runViewModelTest {
        viewModel.setTableLayoutMode(TableLayoutMode.HORIZONTAL_SCROLL)
        advanceUntilIdle()
        assertEquals(TableLayoutMode.HORIZONTAL_SCROLL, fakeRepo.lastTableLayoutMode)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `availableTimezones is populated`() = runViewModelTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.availableTimezones.isNotEmpty())
    }

    @Test
    fun `initial themeMode is SYSTEM`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `initial accentColor is VIOLET`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(AccentColor.VIOLET, viewModel.uiState.value.accentColor)
    }

    @Test
    fun `initial noteCardStyle is TITLE_AND_PREVIEW`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(NoteCardStyle.TITLE_AND_PREVIEW, viewModel.uiState.value.noteCardStyle)
    }

    @Test
    fun `initial tableLayoutMode is FIT_SCREEN`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(TableLayoutMode.FIT_SCREEN, viewModel.uiState.value.tableLayoutMode)
    }

    @Test
    fun `initial protectVaultRecentPreviews is true`() = runViewModelTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.protectVaultRecentPreviews)
    }

    @Test
    fun `setProtectVaultRecentPreviews false is reflected in uiState`() = runViewModelTest {
        viewModel.setProtectVaultRecentPreviews(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.protectVaultRecentPreviews)
    }

    @Test
    fun `setProtectVaultRecentPreviews delegates to repository`() = runViewModelTest {
        viewModel.setProtectVaultRecentPreviews(false)
        advanceUntilIdle()
        assertEquals(false, fakeRepo.lastProtectVaultRecentPreviews)
    }

    @Test
    fun `initial lockVaultOnBackground is true`() = runViewModelTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.lockVaultOnBackground)
    }

    @Test
    fun `setLockVaultOnBackground false is reflected in uiState`() = runViewModelTest {
        viewModel.setLockVaultOnBackground(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.lockVaultOnBackground)
    }

    @Test
    fun `setLockVaultOnBackground delegates to repository`() = runViewModelTest {
        viewModel.setLockVaultOnBackground(false)
        advanceUntilIdle()
        assertEquals(false, fakeRepo.lastLockVaultOnBackground)
    }

    @Test
    fun `initial vaultAutoLockTimeout is IMMEDIATELY`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(
            VaultAutoLockTimeout.IMMEDIATELY,
            viewModel.uiState.value.vaultAutoLockTimeout
        )
    }

    @Test
    fun `setVaultAutoLockTimeout AFTER_5_MINUTES is reflected in uiState`() =
        runViewModelTest {
            viewModel.setVaultAutoLockTimeout(VaultAutoLockTimeout.AFTER_5_MINUTES)
            advanceUntilIdle()

            assertEquals(
                VaultAutoLockTimeout.AFTER_5_MINUTES,
                viewModel.uiState.value.vaultAutoLockTimeout
            )
        }

    @Test
    fun `setVaultAutoLockTimeout delegates to repository`() = runViewModelTest {
        viewModel.setVaultAutoLockTimeout(VaultAutoLockTimeout.AFTER_15_MINUTES)
        advanceUntilIdle()

        assertEquals(VaultAutoLockTimeout.AFTER_15_MINUTES, fakeRepo.lastVaultAutoLockTimeout)
    }

    @Test
    fun `initial unlockVaultWithAndroidCredential is false`() = runViewModelTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.unlockVaultWithAndroidCredential)
    }

    @Test
    fun `setUnlockVaultWithAndroidCredential is ignored before Vault setup`() =
        runViewModelTest {
            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertNull(fakeRepo.lastUnlockVaultWithAndroidCredential)
        }

    @Test
    fun `setUnlockVaultWithAndroidCredential true is reflected for unlocked Vault`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.unlockVaultWithAndroidCredential)
        }

    @Test
    fun `setUnlockVaultWithAndroidCredential delegates to repository for unlocked Vault`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            assertEquals(true, fakeRepo.lastUnlockVaultWithAndroidCredential)
        }

    @Test
    fun `setUnlockVaultWithAndroidCredential true is ignored while Vault is locked`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.LOCKED)
            advanceUntilIdle()

            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertNull(fakeRepo.lastUnlockVaultWithAndroidCredential)
            assertEquals(0L, viewModel.vaultPinChangeState.value.androidCredentialRefreshPromptRequestId)
            assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
        }

    @Test
    fun `setUnlockVaultWithAndroidCredential true requests Android credential refresh`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertEquals(1L, viewModel.vaultPinChangeState.value.androidCredentialRefreshPromptRequestId)
            assertTrue(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
            assertEquals(0, fakeVaultRepo.refreshAndroidCredentialProtectedMaterialCalls)
        }

    @Test
    fun `authenticated Android refresh prompt after enabling refreshes protected material`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            viewModel.onAndroidCredentialRefreshPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(1, fakeVaultRepo.refreshAndroidCredentialProtectedMaterialCalls)
            assertTrue(fakeVaultRepo.hasProtectedMaterialValue)
            assertTrue(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
        }

    @Test
    fun `canceled Android refresh prompt after enabling rolls preference back`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            viewModel.onAndroidCredentialRefreshPromptResult(
                VaultAndroidCredentialPromptResult.CANCELED
            )
            advanceUntilIdle()

            assertEquals(false, fakeRepo.lastUnlockVaultWithAndroidCredential)
            assertFalse(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
        }

    @Test
    fun `failed Android refresh after enabling rolls preference back`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            fakeVaultRepo.refreshAndroidCredentialProtectedMaterialResult =
                RefreshVaultAndroidCredentialProtectedMaterialResult.AuthenticationRequired
            advanceUntilIdle()
            viewModel.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            viewModel.onAndroidCredentialRefreshPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(1, fakeVaultRepo.refreshAndroidCredentialProtectedMaterialCalls)
            assertEquals(false, fakeRepo.lastUnlockVaultWithAndroidCredential)
            assertFalse(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertFalse(fakeVaultRepo.hasProtectedMaterialValue)
            assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
        }

    @Test
    fun `setUnlockVaultWithAndroidCredential false clears protected material`() =
        runViewModelTest {
            fakeVaultRepo.setState(VaultState.LOCKED)
            fakeVaultRepo.setHasProtectedMaterial(true)
            fakeRepo.setUnlockVaultWithAndroidCredential(true)
            advanceUntilIdle()

            viewModel.setUnlockVaultWithAndroidCredential(false)
            advanceUntilIdle()

            assertEquals(false, fakeRepo.lastUnlockVaultWithAndroidCredential)
            assertFalse(fakeVaultRepo.hasProtectedMaterialValue)
            assertEquals(1, fakeVaultRepo.clearAndroidCredentialProtectedMaterialCalls)
        }

    // ── VaultState ────────────────────────────────────────────────────────────

    @Test
    fun `initial vaultState is NOT_CONFIGURED`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(VaultState.NOT_CONFIGURED, viewModel.uiState.value.vaultState)
        assertFalse(viewModel.uiState.value.canChangeVaultPin)
    }

    @Test
    fun `vaultState LOCKED is reflected in uiState`() = runViewModelTest {
        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()
        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        assertFalse(viewModel.uiState.value.canChangeVaultPin)
    }

    @Test
    fun `vaultState UNLOCKED is reflected in uiState`() = runViewModelTest {
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()
        assertEquals(VaultState.UNLOCKED, viewModel.uiState.value.vaultState)
        assertTrue(viewModel.uiState.value.canChangeVaultPin)
    }

    @Test
    fun `vaultState changes are observed over time`() = runViewModelTest {
        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()
        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()
        assertEquals(VaultState.UNLOCKED, viewModel.uiState.value.vaultState)
        fakeVaultRepo.setState(VaultState.NOT_CONFIGURED)
        advanceUntilIdle()
        assertEquals(VaultState.NOT_CONFIGURED, viewModel.uiState.value.vaultState)
    }

    @Test
    fun `lockVault locks when Vault is UNLOCKED`() = runViewModelTest {
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.lockVault()
        advanceUntilIdle()

        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        assertEquals(1, fakeVaultRepo.lockCallCount)
    }

    @Test
    fun `lockVault is ignored when Vault is not UNLOCKED`() = runViewModelTest {
        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()

        viewModel.lockVault()
        advanceUntilIdle()

        assertEquals(VaultState.LOCKED, viewModel.uiState.value.vaultState)
        assertEquals(0, fakeVaultRepo.lockCallCount)
    }

    @Test
    fun `changeVaultPin is ignored while Vault is locked and clears inputs`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        advanceUntilIdle()
        val currentPin = "1234".toCharArray()
        val newPin = "5678".toCharArray()
        val confirmation = "5678".toCharArray()

        viewModel.changeVaultPin(currentPin, newPin, confirmation)
        advanceUntilIdle()

        assertEquals(
            SettingsVaultPinChangeError.VAULT_LOCKED,
            viewModel.vaultPinChangeState.value.error
        )
        assertEquals(0, fakeVaultRepo.changePinCalls)
        assertCleared(currentPin)
        assertCleared(newPin)
        assertCleared(confirmation)
    }

    @Test
    fun `changeVaultPin rejects mismatched confirmation and clears inputs`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        val currentPin = "1234".toCharArray()
        val newPin = "5678".toCharArray()
        val confirmation = "8765".toCharArray()

        viewModel.changeVaultPin(currentPin, newPin, confirmation)
        advanceUntilIdle()

        assertEquals(
            SettingsVaultPinChangeError.PIN_MISMATCH,
            viewModel.vaultPinChangeState.value.error
        )
        assertEquals(0, fakeVaultRepo.changePinCalls)
        assertCleared(currentPin)
        assertCleared(newPin)
        assertCleared(confirmation)
    }

    @Test
    fun `changeVaultPin delegates while Vault is unlocked and clears inputs`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        val currentPin = "1234".toCharArray()
        val newPin = "5678".toCharArray()
        val confirmation = "5678".toCharArray()

        viewModel.changeVaultPin(currentPin, newPin, confirmation)
        advanceUntilIdle()

        assertEquals(1, fakeVaultRepo.changePinCalls)
        assertEquals("1234", fakeVaultRepo.lastChangePinCurrentPin)
        assertEquals("5678", fakeVaultRepo.lastChangePinNewPin)
        assertTrue(viewModel.vaultPinChangeState.value.isSuccessful)
        assertNull(viewModel.vaultPinChangeState.value.error)
        assertEquals(0L, viewModel.vaultPinChangeState.value.androidCredentialRefreshPromptRequestId)
        assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
        assertCleared(currentPin)
        assertCleared(newPin)
        assertCleared(confirmation)
    }

    @Test
    fun `changeVaultPin requests Android credential refresh when Android unlock is enabled`() =
        runViewModelTest {
            fakeRepo.setUnlockVaultWithAndroidCredential(true)
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()

            viewModel.changeVaultPin(
                currentPin = "1234".toCharArray(),
                newPin = "5678".toCharArray(),
                confirmation = "5678".toCharArray()
            )
            advanceUntilIdle()

            assertTrue(viewModel.vaultPinChangeState.value.isSuccessful)
            assertEquals(1L, viewModel.vaultPinChangeState.value.androidCredentialRefreshPromptRequestId)
            assertTrue(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
            assertEquals(0, fakeVaultRepo.refreshAndroidCredentialProtectedMaterialCalls)
        }

    @Test
    fun `authenticated Android refresh prompt refreshes protected material after PIN change`() =
        runViewModelTest {
            fakeRepo.setUnlockVaultWithAndroidCredential(true)
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            viewModel.changeVaultPin(
                currentPin = "1234".toCharArray(),
                newPin = "5678".toCharArray(),
                confirmation = "5678".toCharArray()
            )
            advanceUntilIdle()

            viewModel.onAndroidCredentialRefreshPromptResult(
                VaultAndroidCredentialPromptResult.AUTHENTICATED
            )
            advanceUntilIdle()

            assertEquals(1, fakeVaultRepo.refreshAndroidCredentialProtectedMaterialCalls)
            assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
            assertNull(viewModel.vaultPinChangeState.value.error)
            assertTrue(viewModel.vaultPinChangeState.value.isSuccessful)
        }

    @Test
    fun `canceled Android refresh prompt keeps PIN change successful without refreshing material`() =
        runViewModelTest {
            fakeRepo.setUnlockVaultWithAndroidCredential(true)
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            viewModel.changeVaultPin(
                currentPin = "1234".toCharArray(),
                newPin = "5678".toCharArray(),
                confirmation = "5678".toCharArray()
            )
            advanceUntilIdle()

            viewModel.onAndroidCredentialRefreshPromptResult(
                VaultAndroidCredentialPromptResult.CANCELED
            )
            advanceUntilIdle()

            assertEquals(0, fakeVaultRepo.refreshAndroidCredentialProtectedMaterialCalls)
            assertFalse(viewModel.vaultPinChangeState.value.isAndroidCredentialRefreshPromptPending)
            assertTrue(viewModel.vaultPinChangeState.value.isSuccessful)
        }

    @Test
    fun `changeVaultPin maps wrong current pin without leaking input`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        val currentPin = "0000".toCharArray()
        val newPin = "5678".toCharArray()
        val confirmation = "5678".toCharArray()

        viewModel.changeVaultPin(currentPin, newPin, confirmation)
        advanceUntilIdle()

        assertEquals(1, fakeVaultRepo.changePinCalls)
        assertEquals(
            SettingsVaultPinChangeError.WRONG_CURRENT_PIN,
            viewModel.vaultPinChangeState.value.error
        )
        assertFalse(viewModel.vaultPinChangeState.value.isSuccessful)
        assertCleared(currentPin)
        assertCleared(newPin)
        assertCleared(confirmation)
    }

    @Test
    fun `clearVaultPinChangeFeedback clears transient result`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        viewModel.changeVaultPin(
            currentPin = "1234".toCharArray(),
            newPin = "5678".toCharArray(),
            confirmation = "5678".toCharArray()
        )
        advanceUntilIdle()
        assertTrue(viewModel.vaultPinChangeState.value.isSuccessful)

        viewModel.clearVaultPinChangeFeedback()

        assertFalse(viewModel.vaultPinChangeState.value.isSuccessful)
        assertNull(viewModel.vaultPinChangeState.value.error)
    }

    // ── Vault reset ───────────────────────────────────────────────────────────

    @Test
    fun `requestVaultReset is blocked when Vault is locked`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        advanceUntilIdle()

        viewModel.requestVaultReset()
        advanceUntilIdle()

        assertFalse(viewModel.vaultResetState.value.isConfirmationVisible)
        assertFalse(viewModel.vaultResetState.value.isBusy)
        assertEquals(
            SettingsVaultResetError.VAULT_LOCKED,
            viewModel.vaultResetState.value.error
        )
        assertFalse(viewModel.vaultResetState.value.isSuccessful)
        assertEquals(0, fakeVaultRepo.resetVaultCalls)
    }

    @Test
    fun `requestVaultReset shows confirmation when Vault is unlocked`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()

        viewModel.requestVaultReset()
        advanceUntilIdle()

        assertTrue(viewModel.vaultResetState.value.isConfirmationVisible)
        assertEquals(0, fakeVaultRepo.resetVaultCalls)
    }

    @Test
    fun `requestVaultReset is ignored without configured Vault`() = runViewModelTest {
        advanceUntilIdle()

        viewModel.requestVaultReset()
        advanceUntilIdle()

        assertFalse(viewModel.vaultResetState.value.isConfirmationVisible)
        assertEquals(
            SettingsVaultResetError.VAULT_NOT_CONFIGURED,
            viewModel.vaultResetState.value.error
        )
        assertEquals(0, fakeVaultRepo.resetVaultCalls)
    }

    @Test
    fun `cancelVaultReset hides confirmation without invoking reset`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        viewModel.requestVaultReset()
        advanceUntilIdle()
        assertTrue(viewModel.vaultResetState.value.isConfirmationVisible)

        viewModel.cancelVaultReset()
        advanceUntilIdle()

        assertFalse(viewModel.vaultResetState.value.isConfirmationVisible)
        assertEquals(0, fakeVaultRepo.resetVaultCalls)
        assertNull(viewModel.vaultResetState.value.error)
        assertFalse(viewModel.vaultResetState.value.isSuccessful)
    }

    @Test
    fun `confirmVaultReset without prior request does not invoke reset`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        advanceUntilIdle()

        viewModel.confirmVaultReset()
        advanceUntilIdle()

        assertEquals(0, fakeVaultRepo.resetVaultCalls)
        assertFalse(viewModel.vaultResetState.value.isBusy)
        assertFalse(viewModel.vaultResetState.value.isSuccessful)
        assertNull(viewModel.vaultResetState.value.error)
    }

    @Test
    fun `confirmVaultReset is blocked if Vault locks after confirmation`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        viewModel.requestVaultReset()
        advanceUntilIdle()
        assertTrue(viewModel.vaultResetState.value.isConfirmationVisible)

        fakeVaultRepo.lock()
        advanceUntilIdle()
        viewModel.confirmVaultReset()
        advanceUntilIdle()

        assertEquals(0, fakeVaultRepo.resetVaultCalls)
        assertFalse(viewModel.vaultResetState.value.isConfirmationVisible)
        assertEquals(
            SettingsVaultResetError.VAULT_LOCKED,
            viewModel.vaultResetState.value.error
        )
        assertFalse(viewModel.vaultResetState.value.isSuccessful)
    }

    @Test
    fun `confirmVaultReset success clears state and reports success`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        viewModel.requestVaultReset()
        advanceUntilIdle()

        viewModel.confirmVaultReset()
        advanceUntilIdle()

        assertEquals(1, fakeVaultRepo.resetVaultCalls)
        assertFalse(viewModel.vaultResetState.value.isBusy)
        assertFalse(viewModel.vaultResetState.value.isConfirmationVisible)
        assertTrue(viewModel.vaultResetState.value.isSuccessful)
        assertNull(viewModel.vaultResetState.value.error)
    }

    @Test
    fun `confirmVaultReset disables Android credential preference before resetting`() =
        runViewModelTest {
            fakeRepo.setUnlockVaultWithAndroidCredential(true)
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.unlockVaultWithAndroidCredential)

            viewModel.requestVaultReset()
            advanceUntilIdle()
            viewModel.confirmVaultReset()
            advanceUntilIdle()

            assertEquals(false, fakeRepo.lastUnlockVaultWithAndroidCredential)
            assertFalse(viewModel.uiState.value.unlockVaultWithAndroidCredential)
            assertEquals(1, fakeVaultRepo.resetVaultCalls)
            assertTrue(viewModel.vaultResetState.value.isSuccessful)
        }

    @Test
    fun `confirmVaultReset does not touch Android credential preference when disabled`() =
        runViewModelTest {
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            viewModel.requestVaultReset()
            advanceUntilIdle()

            viewModel.confirmVaultReset()
            advanceUntilIdle()

            assertNull(fakeRepo.lastUnlockVaultWithAndroidCredential)
            assertEquals(1, fakeVaultRepo.resetVaultCalls)
            assertTrue(viewModel.vaultResetState.value.isSuccessful)
        }

    @Test
    fun `confirmVaultReset failure maps to OPERATION_FAILED and stays configured`() =
        runViewModelTest {
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            fakeVaultRepo.resetVaultResult = ResetVaultResult.Failed

            viewModel.requestVaultReset()
            advanceUntilIdle()
            viewModel.confirmVaultReset()
            advanceUntilIdle()

            assertEquals(1, fakeVaultRepo.resetVaultCalls)
            assertFalse(viewModel.vaultResetState.value.isBusy)
            assertEquals(
                SettingsVaultResetError.OPERATION_FAILED,
                viewModel.vaultResetState.value.error
            )
            assertFalse(viewModel.vaultResetState.value.isSuccessful)
        }

    @Test
    fun `confirmVaultReset VaultNotConfigured result is mapped to specific error`() =
        runViewModelTest {
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            fakeVaultRepo.resetVaultResult = ResetVaultResult.VaultNotConfigured

            viewModel.requestVaultReset()
            advanceUntilIdle()
            viewModel.confirmVaultReset()
            advanceUntilIdle()

            assertEquals(1, fakeVaultRepo.resetVaultCalls)
            assertEquals(
                SettingsVaultResetError.VAULT_NOT_CONFIGURED,
                viewModel.vaultResetState.value.error
            )
            assertFalse(viewModel.vaultResetState.value.isSuccessful)
        }

    @Test
    fun `confirmVaultReset VaultLocked result is mapped to locked error`() =
        runViewModelTest {
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeVaultRepo.unlockWithPin("1234".toCharArray())
            advanceUntilIdle()
            fakeVaultRepo.resetVaultResult = ResetVaultResult.VaultLocked

            viewModel.requestVaultReset()
            advanceUntilIdle()
            viewModel.confirmVaultReset()
            advanceUntilIdle()

            assertEquals(1, fakeVaultRepo.resetVaultCalls)
            assertEquals(
                SettingsVaultResetError.VAULT_LOCKED,
                viewModel.vaultResetState.value.error
            )
            assertFalse(viewModel.vaultResetState.value.isSuccessful)
        }

    @Test
    fun `confirmVaultReset maps thrown exception to OPERATION_FAILED`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        fakeVaultRepo.resetVaultShouldThrow = true

        viewModel.requestVaultReset()
        advanceUntilIdle()
        viewModel.confirmVaultReset()
        advanceUntilIdle()

        assertEquals(1, fakeVaultRepo.resetVaultCalls)
        assertFalse(viewModel.vaultResetState.value.isBusy)
        assertEquals(
            SettingsVaultResetError.OPERATION_FAILED,
            viewModel.vaultResetState.value.error
        )
        assertFalse(viewModel.vaultResetState.value.isSuccessful)
    }

    @Test
    fun `clearVaultResetFeedback clears transient feedback`() = runViewModelTest {
        fakeVaultRepo.configureStoredPin("1234".toCharArray())
        fakeVaultRepo.unlockWithPin("1234".toCharArray())
        advanceUntilIdle()
        viewModel.requestVaultReset()
        advanceUntilIdle()
        viewModel.confirmVaultReset()
        advanceUntilIdle()
        assertTrue(viewModel.vaultResetState.value.isSuccessful)

        viewModel.clearVaultResetFeedback()
        advanceUntilIdle()

        assertFalse(viewModel.vaultResetState.value.isSuccessful)
        assertNull(viewModel.vaultResetState.value.error)
    }

    // ── Delete all stored notes ──────────────────────────────────────────────

    @Test
    fun `delete all normal notes does not require Vault authentication`() =
        runViewModelTest {
            fakeStoredNoteRepo.setCount(4)
            advanceUntilIdle()

            viewModel.requestDeleteAllNotes()
            viewModel.confirmDeleteAllNotes(CharArray(0))
            advanceUntilIdle()

            assertEquals(1, fakeStoredNoteRepo.deleteAllCalls)
            assertEquals(0, fakeStoredVaultNoteRepo.deleteAllCalls)
            assertTrue(viewModel.deleteAllNotesState.value.isSuccessful)
            assertEquals(0, viewModel.deleteAllNotesState.value.totalNoteCount)
        }

    @Test
    fun `wrong Vault PIN prevents deletion of normal and Vault notes`() =
        runViewModelTest {
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeStoredNoteRepo.setCount(3)
            fakeStoredVaultNoteRepo.setCount(2)
            advanceUntilIdle()
            viewModel.requestDeleteAllNotes()
            val wrongPin = "0000".toCharArray()

            viewModel.confirmDeleteAllNotes(wrongPin)
            advanceUntilIdle()

            assertCleared(wrongPin)
            assertEquals(0, fakeStoredNoteRepo.deleteAllCalls)
            assertEquals(0, fakeStoredVaultNoteRepo.deleteAllCalls)
            assertEquals(
                SettingsDeleteAllNotesError.WRONG_VAULT_PIN,
                viewModel.deleteAllNotesState.value.error
            )
            assertTrue(viewModel.deleteAllNotesState.value.isConfirmationVisible)
        }

    @Test
    fun `correct Vault PIN deletes both note stores and preserves Vault configuration`() =
        runViewModelTest {
            fakeVaultRepo.configureStoredPin("1234".toCharArray())
            fakeStoredNoteRepo.setCount(3)
            fakeStoredVaultNoteRepo.setCount(2)
            advanceUntilIdle()
            viewModel.requestDeleteAllNotes()
            val pin = "1234".toCharArray()

            viewModel.confirmDeleteAllNotes(pin)
            advanceUntilIdle()

            assertCleared(pin)
            assertEquals(1, fakeStoredNoteRepo.deleteAllCalls)
            assertEquals(1, fakeStoredVaultNoteRepo.deleteAllCalls)
            assertTrue(viewModel.deleteAllNotesState.value.isSuccessful)
            assertEquals(VaultState.LOCKED, fakeVaultRepo.state.first())
            assertEquals(1, fakeVaultRepo.lockCallCount)
        }

    private fun assertCleared(pin: CharArray) {
        assertTrue(pin.all { it == '\u0000' })
    }
}

private class FakeStoredNoteRepository : NoteRepository {
    private val normalCount = MutableStateFlow(0)

    override val allNotes: Flow<List<Note>> = flowOf(emptyList())
    override val allNotesSortedAsc: Flow<List<Note>> = flowOf(emptyList())
    override val deletedNotes: Flow<List<Note>> = flowOf(emptyList())
    override val noteLinkCandidates: Flow<List<NoteLinkCandidate>> = flowOf(emptyList())
    override val distinctActiveDays: Flow<Set<Long>> = flowOf(emptySet())
    override val distinctLocalDays: Flow<Set<Long>> = flowOf(emptySet())
    override val allNormalNoteCount: Flow<Int> = normalCount
    var deleteAllCalls = 0
        private set

    fun setCount(value: Int) {
        normalCount.value = value
    }

    override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
    override fun searchNotesScored(query: String): Flow<List<ScoredNote>> = flowOf(emptyList())
    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>> =
        flowOf(emptyList())
    override suspend fun getNoteById(id: Long): Note? = null
    override suspend fun saveNote(note: Note): Long = note.id
    override suspend fun moveToTrash(id: Long) = Unit
    override suspend fun restoreFromTrash(id: Long) = Unit
    override suspend fun deleteNotePermanently(id: Long) = Unit
    override suspend fun emptyTrash() = Unit
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
    override suspend fun deleteAllNormalNotesPermanently(): Int {
        deleteAllCalls += 1
        val deleted = normalCount.value
        normalCount.value = 0
        return deleted
    }
}

private class FakeStoredVaultNoteRepository : VaultNoteRepository {
    private val vaultCount = MutableStateFlow(0)

    override val vaultNotes: Flow<List<Note>> = flowOf(emptyList())
    override val vaultTrashedNotes: Flow<List<Note>> = flowOf(emptyList())
    override val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>> = flowOf(emptyList())
    override val allVaultNoteCount: Flow<Int> = vaultCount
    var deleteAllCalls = 0
        private set

    fun setCount(value: Int) {
        vaultCount.value = value
    }

    override suspend fun getVaultNoteById(id: Long): Note? = null
    override suspend fun saveVaultNote(note: Note): Long = note.id
    override suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult =
        DuplicateVaultNoteResult.NotFound
    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult =
        MoveNoteToVaultResult.NotFound
    override suspend fun removeNoteFromVault(id: Long): Boolean = false
    override suspend fun moveVaultNoteToTrash(id: Long): Boolean = false
    override suspend fun restoreVaultNoteFromTrash(id: Long): Boolean = false
    override suspend fun deleteVaultNotePermanently(id: Long): Boolean = false
    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? = null
    override suspend fun deleteAllVaultNotesPermanently(): Int {
        deleteAllCalls += 1
        val deleted = vaultCount.value
        vaultCount.value = 0
        return deleted
    }
}

private class FakeNoteStatisticsRepository : NoteStatisticsRepository {
    override val indexedNotes: Flow<List<IndexedNoteStatistics>> = flowOf(emptyList())
    override val indexState: Flow<NoteStatisticsIndexState> =
        flowOf(NoteStatisticsIndexState())

    override suspend fun rebuildIndex() = Unit
}

// ── Fake ─────────────────────────────────────────────────────────────────────

private class FakePreferencesRepository : IUserPreferencesRepository {

    private val _themeMode     = MutableStateFlow(ThemeMode.SYSTEM)
    private val _fontScale     = MutableStateFlow(FontScale.NORMAL)
    private val _timezoneId    = MutableStateFlow("")
    private val _accentColor   = MutableStateFlow(AccentColor.VIOLET)
    private val _noteCardStyle = MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)
    private val _tableLayoutMode = MutableStateFlow(TableLayoutMode.FIT_SCREEN)
    private val _protectVaultRecentPreviews = MutableStateFlow(true)
    private val _lockVaultOnBackground = MutableStateFlow(true)
    private val _vaultAutoLockTimeout =
        MutableStateFlow(VaultAutoLockTimeout.IMMEDIATELY)
    private val _unlockVaultWithAndroidCredential = MutableStateFlow(false)

    var lastThemeMode:     ThemeMode?     = null
    var lastFontScale:     FontScale?     = null
    var lastTimezoneId:    String?        = null
    var lastAccentColor:   AccentColor?   = null
    var lastNoteCardStyle: NoteCardStyle? = null
    var lastTableLayoutMode: TableLayoutMode? = null
    var lastProtectVaultRecentPreviews: Boolean? = null
    var lastLockVaultOnBackground: Boolean? = null
    var lastVaultAutoLockTimeout: VaultAutoLockTimeout? = null
    var lastUnlockVaultWithAndroidCredential: Boolean? = null

    override val themeMode:     Flow<ThemeMode>     = _themeMode
    override val fontScale:     Flow<FontScale>     = _fontScale
    override val timezoneId:    Flow<String>        = _timezoneId
    override val accentColor:   Flow<AccentColor>   = _accentColor
    override val noteCardStyle: Flow<NoteCardStyle> = _noteCardStyle
    override val tableLayoutMode: Flow<TableLayoutMode> = _tableLayoutMode
    override val protectVaultRecentPreviews: Flow<Boolean> = _protectVaultRecentPreviews
    override val lockVaultOnBackground: Flow<Boolean> = _lockVaultOnBackground
    override val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout> = _vaultAutoLockTimeout
    override val unlockVaultWithAndroidCredential: Flow<Boolean> =
        _unlockVaultWithAndroidCredential

    override suspend fun setThemeMode(mode: ThemeMode) {
        lastThemeMode    = mode
        _themeMode.value = mode
    }

    override suspend fun setFontScale(scale: FontScale) {
        lastFontScale    = scale
        _fontScale.value = scale
    }

    override suspend fun setTimezoneId(id: String) {
        lastTimezoneId    = id
        _timezoneId.value = id
    }

    override suspend fun setAccentColor(color: AccentColor) {
        lastAccentColor    = color
        _accentColor.value = color
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        lastNoteCardStyle    = style
        _noteCardStyle.value = style
    }

    override suspend fun setTableLayoutMode(mode: TableLayoutMode) {
        lastTableLayoutMode = mode
        _tableLayoutMode.value = mode
    }

    override suspend fun setProtectVaultRecentPreviews(value: Boolean) {
        lastProtectVaultRecentPreviews = value
        _protectVaultRecentPreviews.value = value
    }

    override suspend fun setLockVaultOnBackground(value: Boolean) {
        lastLockVaultOnBackground = value
        _lockVaultOnBackground.value = value
    }

    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) {
        lastVaultAutoLockTimeout = timeout
        _vaultAutoLockTimeout.value = timeout
    }

    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) {
        lastUnlockVaultWithAndroidCredential = value
        _unlockVaultWithAndroidCredential.value = value
    }
}

private class FakeVaultRepository : VaultRepository {

    private val _state = MutableStateFlow(VaultState.NOT_CONFIGURED)
    private val _hasProtectedMaterial = MutableStateFlow(false)
    private var storedPin: CharArray? = null

    override val state: Flow<VaultState> = _state
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        _hasProtectedMaterial
    var lockCallCount: Int = 0
    var changePinCalls: Int = 0
        private set
    var refreshAndroidCredentialProtectedMaterialCalls: Int = 0
        private set
    var clearAndroidCredentialProtectedMaterialCalls: Int = 0
        private set
    val hasProtectedMaterialValue: Boolean
        get() = _hasProtectedMaterial.value
    var refreshAndroidCredentialProtectedMaterialResult:
        RefreshVaultAndroidCredentialProtectedMaterialResult =
        RefreshVaultAndroidCredentialProtectedMaterialResult.Success
    var lastChangePinCurrentPin: String? = null
        private set
    var lastChangePinNewPin: String? = null
        private set
    var resetVaultCalls: Int = 0
        private set
    var resetVaultResult: ResetVaultResult = ResetVaultResult.Success
    var resetVaultShouldThrow: Boolean = false

    fun setState(value: VaultState) {
        _state.value = value
    }

    override suspend fun configurePin(pin: CharArray) {
        configureStoredPin(pin)
        _state.value = VaultState.LOCKED
    }

    override suspend fun unlockWithPin(pin: CharArray): Boolean {
        val unlocked = storedPin?.contentEquals(pin) == true
        _state.value = if (unlocked) VaultState.UNLOCKED else VaultState.LOCKED
        if (unlocked) {
            _hasProtectedMaterial.value = true
        }
        return unlocked
    }

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult {
        refreshAndroidCredentialProtectedMaterialCalls += 1
        if (
            refreshAndroidCredentialProtectedMaterialResult ==
            RefreshVaultAndroidCredentialProtectedMaterialResult.Success
        ) {
            _hasProtectedMaterial.value = true
        }
        return refreshAndroidCredentialProtectedMaterialResult
    }

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() {
        clearAndroidCredentialProtectedMaterialCalls += 1
        _hasProtectedMaterial.value = false
    }

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult {
        changePinCalls += 1
        lastChangePinCurrentPin = currentPin.concatToString()
        lastChangePinNewPin = newPin.concatToString()

        return when {
            storedPin == null -> ChangeVaultPinResult.VaultNotConfigured
            _state.value != VaultState.UNLOCKED -> ChangeVaultPinResult.VaultLocked
            storedPin?.contentEquals(currentPin) != true -> ChangeVaultPinResult.WrongCurrentPin
            newPin.isEmpty() -> ChangeVaultPinResult.InvalidNewPin
            else -> {
                storedPin?.fill('\u0000')
                storedPin = newPin.copyOf()
                _hasProtectedMaterial.value = false
                ChangeVaultPinResult.Success
            }
        }
    }

    override suspend fun resetVault(): ResetVaultResult {
        resetVaultCalls += 1
        if (resetVaultShouldThrow) {
            throw RuntimeException("simulated reset failure")
        }
        if (_state.value == VaultState.LOCKED) {
            return ResetVaultResult.VaultLocked
        }
        if (resetVaultResult == ResetVaultResult.Success) {
            storedPin?.fill('\u0000')
            storedPin = null
            _hasProtectedMaterial.value = false
            _state.value = VaultState.NOT_CONFIGURED
        }
        return resetVaultResult
    }

    override fun lock() {
        lockCallCount++
        if (_state.value == VaultState.UNLOCKED) {
            _state.value = VaultState.LOCKED
        }
    }

    fun configureStoredPin(pin: CharArray) {
        storedPin?.fill('\u0000')
        storedPin = pin.copyOf()
        _hasProtectedMaterial.value = false
        _state.value = VaultState.LOCKED
    }

    fun setHasProtectedMaterial(value: Boolean) {
        _hasProtectedMaterial.value = value
    }
}
