package io.github.r0x4nk.nexnote.ui.screen.settings

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveLeftHandedUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetLeftHandedUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetTimezoneIdUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakePreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo  = FakePreferencesRepository()
        viewModel = SettingsViewModel(
            observeThemeMode = ObserveThemeModeUseCase(fakeRepo),
            observeFontScale = ObserveFontScaleUseCase(fakeRepo),
            observeTimezoneId = ObserveTimezoneIdUseCase(fakeRepo),
            observeLeftHanded = ObserveLeftHandedUseCase(fakeRepo),
            observeAccentColor = ObserveAccentColorUseCase(fakeRepo),
            observeNoteCardStyle = ObserveNoteCardStyleUseCase(fakeRepo),
            setThemeModeUseCase = SetThemeModeUseCase(fakeRepo),
            setFontScaleUseCase = SetFontScaleUseCase(fakeRepo),
            setTimezoneIdUseCase = SetTimezoneIdUseCase(fakeRepo),
            setLeftHandedUseCase = SetLeftHandedUseCase(fakeRepo),
            setAccentColorUseCase = SetAccentColorUseCase(fakeRepo),
            setNoteCardStyleUseCase = SetNoteCardStyleUseCase(fakeRepo)
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

    // ── isLeftHanded ──────────────────────────────────────────────────────────

    @Test
    fun `setLeftHanded true is reflected in uiState`() = runViewModelTest {
        viewModel.setLeftHanded(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLeftHanded)
    }

    @Test
    fun `setLeftHanded false is reflected in uiState`() = runViewModelTest {
        fakeRepo.setLeftHanded(true)   // start with true
        viewModel.setLeftHanded(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLeftHanded)
    }

    @Test
    fun `setLeftHanded delegates to repository`() = runViewModelTest {
        viewModel.setLeftHanded(true)
        advanceUntilIdle()
        assertEquals(true, fakeRepo.lastIsLeftHanded)
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
    fun `initial isLeftHanded is false`() = runViewModelTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLeftHanded)
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
}

// ── Fake ─────────────────────────────────────────────────────────────────────

private class FakePreferencesRepository : IUserPreferencesRepository {

    private val _themeMode     = MutableStateFlow(ThemeMode.SYSTEM)
    private val _fontScale     = MutableStateFlow(FontScale.NORMAL)
    private val _timezoneId    = MutableStateFlow("")
    private val _isLeftHanded  = MutableStateFlow(false)
    private val _accentColor   = MutableStateFlow(AccentColor.VIOLET)
    private val _noteCardStyle = MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)

    var lastThemeMode:     ThemeMode?     = null
    var lastFontScale:     FontScale?     = null
    var lastTimezoneId:    String?        = null
    var lastIsLeftHanded:  Boolean?       = null
    var lastAccentColor:   AccentColor?   = null
    var lastNoteCardStyle: NoteCardStyle? = null

    override val themeMode:     Flow<ThemeMode>     = _themeMode
    override val fontScale:     Flow<FontScale>     = _fontScale
    override val timezoneId:    Flow<String>        = _timezoneId
    override val isLeftHanded:  Flow<Boolean>       = _isLeftHanded
    override val accentColor:   Flow<AccentColor>   = _accentColor
    override val noteCardStyle: Flow<NoteCardStyle> = _noteCardStyle

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

    override suspend fun setLeftHanded(value: Boolean) {
        lastIsLeftHanded    = value
        _isLeftHanded.value = value
    }

    override suspend fun setAccentColor(color: AccentColor) {
        lastAccentColor    = color
        _accentColor.value = color
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        lastNoteCardStyle    = style
        _noteCardStyle.value = style
    }
}
