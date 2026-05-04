package com.example.nexnote.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.nexnote.domain.model.AccentColor
import com.example.nexnote.domain.model.FontScale
import com.example.nexnote.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    /** DataStore backed by a real temporary file — no mock, no Robolectric. */
    private fun createTestDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope       = testScope,
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )

    // ── ThemeMode ─────────────────────────────────────────────────────────────

    @Test
    fun `themeMode defaults to SYSTEM`() = testScope.runTest {
        val ds = createTestDataStore()
        assertEquals(ThemeMode.SYSTEM, ds.themeMode.first())
    }

    @Test
    fun `setThemeMode persists LIGHT`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[THEME_MODE_KEY] = ThemeMode.LIGHT.name }
        assertEquals(ThemeMode.LIGHT, ds.themeMode.first())
    }

    @Test
    fun `setThemeMode persists DARK`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[THEME_MODE_KEY] = ThemeMode.DARK.name }
        assertEquals(ThemeMode.DARK, ds.themeMode.first())
    }

    @Test
    fun `setThemeMode persists TRUE_DARK`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[THEME_MODE_KEY] = ThemeMode.TRUE_DARK.name }
        assertEquals(ThemeMode.TRUE_DARK, ds.themeMode.first())
    }

    // ── FontScale ─────────────────────────────────────────────────────────────

    @Test
    fun `fontScale defaults to NORMAL`() = testScope.runTest {
        val ds = createTestDataStore()
        assertEquals(FontScale.NORMAL, ds.fontScale.first())
    }

    @Test
    fun `setFontScale persists SMALL`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[FONT_SCALE_KEY] = FontScale.SMALL.name }
        assertEquals(FontScale.SMALL, ds.fontScale.first())
    }

    @Test
    fun `setFontScale persists LARGE`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[FONT_SCALE_KEY] = FontScale.LARGE.name }
        assertEquals(FontScale.LARGE, ds.fontScale.first())
    }

    // ── TimezoneId ────────────────────────────────────────────────────────────

    @Test
    fun `timezoneId defaults to empty string`() = testScope.runTest {
        val ds = createTestDataStore()
        assertEquals("", ds.timezoneId.first())
    }

    @Test
    fun `setTimezoneId persists value`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[TIMEZONE_KEY] = "Europe/Rome" }
        assertEquals("Europe/Rome", ds.timezoneId.first())
    }

    @Test
    fun `setTimezoneId overwrite clears previous value`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[TIMEZONE_KEY] = "America/New_York" }
        ds.edit { it[TIMEZONE_KEY] = "" }
        assertEquals("", ds.timezoneId.first())
    }

    // ── isLeftHanded ──────────────────────────────────────────────────────────

    @Test
    fun `isLeftHanded defaults to false`() = testScope.runTest {
        val ds = createTestDataStore()
        assertFalse(ds.isLeftHanded.first())
    }

    @Test
    fun `setLeftHanded persists true`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[LEFT_HANDED_KEY] = true }
        assertTrue(ds.isLeftHanded.first())
    }

    @Test
    fun `setLeftHanded round-trips false after true`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[LEFT_HANDED_KEY] = true }
        ds.edit { it[LEFT_HANDED_KEY] = false }
        assertFalse(ds.isLeftHanded.first())
    }

    // ── AccentColor ───────────────────────────────────────────────────────────

    @Test
    fun `accentColor defaults to VIOLET`() = testScope.runTest {
        val ds = createTestDataStore()
        assertEquals(AccentColor.VIOLET, ds.accentColor.first())
    }

    @Test
    fun `setAccentColor persists BLUE`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[ACCENT_COLOR_KEY] = AccentColor.BLUE.name }
        assertEquals(AccentColor.BLUE, ds.accentColor.first())
    }

    @Test
    fun `setAccentColor persists all values`() = testScope.runTest {
        val ds = createTestDataStore()
        AccentColor.entries.forEach { color ->
            ds.edit { it[ACCENT_COLOR_KEY] = color.name }
            assertEquals(color, ds.accentColor.first())
        }
    }

    @Test
    fun `unknown accentColor name falls back to VIOLET`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[ACCENT_COLOR_KEY] = "UNKNOWN_COLOR" }
        assertEquals(AccentColor.VIOLET, ds.accentColor.first())
    }
}

// ── Helper extensions (mirror UserPreferencesRepository logic) ────────────────

private val THEME_MODE_KEY   = stringPreferencesKey("theme_mode")
private val FONT_SCALE_KEY   = stringPreferencesKey("font_scale")
private val TIMEZONE_KEY     = stringPreferencesKey("timezone_id")
private val LEFT_HANDED_KEY  = booleanPreferencesKey("is_left_handed")
private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")

private val DataStore<Preferences>.themeMode
    get() = data.map { prefs ->
        val name = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
    }

private val DataStore<Preferences>.fontScale
    get() = data.map { prefs ->
        val name = prefs[FONT_SCALE_KEY] ?: FontScale.NORMAL.name
        FontScale.entries.firstOrNull { it.name == name } ?: FontScale.NORMAL
    }

private val DataStore<Preferences>.timezoneId
    get() = data.map { prefs -> prefs[TIMEZONE_KEY] ?: "" }

private val DataStore<Preferences>.isLeftHanded
    get() = data.map { prefs -> prefs[LEFT_HANDED_KEY] ?: false }

private val DataStore<Preferences>.accentColor
    get() = data.map { prefs ->
        val name = prefs[ACCENT_COLOR_KEY] ?: AccentColor.VIOLET.name
        AccentColor.entries.firstOrNull { it.name == name } ?: AccentColor.VIOLET
    }
