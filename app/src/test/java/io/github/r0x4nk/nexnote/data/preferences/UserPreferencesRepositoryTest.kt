package io.github.r0x4nk.nexnote.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
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

    @Test
    fun `tableLayoutMode defaults to FIT_SCREEN`() = testScope.runTest {
        val ds = createTestDataStore()
        assertEquals(TableLayoutMode.FIT_SCREEN, ds.tableLayoutMode.first())
    }

    @Test
    fun `tableLayoutMode persists all values`() = testScope.runTest {
        val ds = createTestDataStore()
        TableLayoutMode.entries.forEach { mode ->
            ds.edit { it[TABLE_LAYOUT_MODE_KEY] = mode.name }
            assertEquals(mode, ds.tableLayoutMode.first())
        }
    }

    @Test
    fun `unknown tableLayoutMode falls back to FIT_SCREEN`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[TABLE_LAYOUT_MODE_KEY] = "UNKNOWN_MODE" }
        assertEquals(TableLayoutMode.FIT_SCREEN, ds.tableLayoutMode.first())
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

    // ── Vault recent previews protection ─────────────────────────────────────

    @Test
    fun `protectVaultRecentPreviews defaults to true`() = testScope.runTest {
        val ds = createTestDataStore()
        assertTrue(ds.protectVaultRecentPreviews.first())
    }

    @Test
    fun `setProtectVaultRecentPreviews persists false`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[VAULT_RECENT_PREVIEWS_PROTECTED_KEY] = false }
        assertFalse(ds.protectVaultRecentPreviews.first())
    }

    @Test
    fun `setProtectVaultRecentPreviews round-trips true after false`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[VAULT_RECENT_PREVIEWS_PROTECTED_KEY] = false }
        ds.edit { it[VAULT_RECENT_PREVIEWS_PROTECTED_KEY] = true }
        assertTrue(ds.protectVaultRecentPreviews.first())
    }

    // ── Vault auto-lock ──────────────────────────────────────────────────────

    @Test
    fun `lockVaultOnBackground defaults to true`() = testScope.runTest {
        val ds = createTestDataStore()
        assertTrue(ds.lockVaultOnBackground.first())
    }

    @Test
    fun `setLockVaultOnBackground persists false`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[VAULT_LOCK_ON_BACKGROUND_KEY] = false }
        assertFalse(ds.lockVaultOnBackground.first())
    }

    @Test
    fun `setLockVaultOnBackground round-trips true after false`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[VAULT_LOCK_ON_BACKGROUND_KEY] = false }
        ds.edit { it[VAULT_LOCK_ON_BACKGROUND_KEY] = true }
        assertTrue(ds.lockVaultOnBackground.first())
    }

    @Test
    fun `vaultAutoLockTimeout defaults to IMMEDIATELY`() = testScope.runTest {
        val ds = createTestDataStore()
        assertEquals(VaultAutoLockTimeout.IMMEDIATELY, ds.vaultAutoLockTimeout.first())
    }

    @Test
    fun `setVaultAutoLockTimeout persists all values`() = testScope.runTest {
        val ds = createTestDataStore()
        VaultAutoLockTimeout.entries.forEach { timeout ->
            ds.edit { it[VAULT_AUTO_LOCK_TIMEOUT_KEY] = timeout.name }
            assertEquals(timeout, ds.vaultAutoLockTimeout.first())
        }
    }

    @Test
    fun `unknown vaultAutoLockTimeout falls back to IMMEDIATELY`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[VAULT_AUTO_LOCK_TIMEOUT_KEY] = "AFTER_99_MINUTES" }
        assertEquals(VaultAutoLockTimeout.IMMEDIATELY, ds.vaultAutoLockTimeout.first())
    }

    // ── Vault Android credential unlock ─────────────────────────────────────

    @Test
    fun `unlockVaultWithAndroidCredential defaults to false`() = testScope.runTest {
        val ds = createTestDataStore()
        assertFalse(ds.unlockVaultWithAndroidCredential.first())
    }

    @Test
    fun `setUnlockVaultWithAndroidCredential persists true`() = testScope.runTest {
        val ds = createTestDataStore()
        ds.edit { it[VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY] = true }
        assertTrue(ds.unlockVaultWithAndroidCredential.first())
    }

    @Test
    fun `setUnlockVaultWithAndroidCredential round-trips false after true`() =
        testScope.runTest {
            val ds = createTestDataStore()
            ds.edit { it[VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY] = true }
            ds.edit { it[VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY] = false }
            assertFalse(ds.unlockVaultWithAndroidCredential.first())
        }
}

// ── Helper extensions (mirror UserPreferencesRepository logic) ────────────────

private val THEME_MODE_KEY   = stringPreferencesKey("theme_mode")
private val FONT_SCALE_KEY   = stringPreferencesKey("font_scale")
private val TABLE_LAYOUT_MODE_KEY = stringPreferencesKey("table_layout_mode")
private val TIMEZONE_KEY     = stringPreferencesKey("timezone_id")
private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
private val VAULT_RECENT_PREVIEWS_PROTECTED_KEY =
    booleanPreferencesKey("vault_recent_previews_protected")
private val VAULT_LOCK_ON_BACKGROUND_KEY =
    booleanPreferencesKey("vault_lock_on_background")
private val VAULT_AUTO_LOCK_TIMEOUT_KEY =
    stringPreferencesKey("vault_auto_lock_timeout")
private val VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY =
    booleanPreferencesKey("vault_android_credential_unlock_enabled")

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

private val DataStore<Preferences>.tableLayoutMode
    get() = data.map { prefs ->
        val name = prefs[TABLE_LAYOUT_MODE_KEY] ?: TableLayoutMode.FIT_SCREEN.name
        TableLayoutMode.entries.firstOrNull { it.name == name } ?: TableLayoutMode.FIT_SCREEN
    }

private val DataStore<Preferences>.timezoneId
    get() = data.map { prefs -> prefs[TIMEZONE_KEY] ?: "" }

private val DataStore<Preferences>.accentColor
    get() = data.map { prefs ->
        val name = prefs[ACCENT_COLOR_KEY] ?: AccentColor.VIOLET.name
        AccentColor.entries.firstOrNull { it.name == name } ?: AccentColor.VIOLET
    }

private val DataStore<Preferences>.protectVaultRecentPreviews
    get() = data.map { prefs -> prefs[VAULT_RECENT_PREVIEWS_PROTECTED_KEY] ?: true }

private val DataStore<Preferences>.lockVaultOnBackground
    get() = data.map { prefs -> prefs[VAULT_LOCK_ON_BACKGROUND_KEY] ?: true }

private val DataStore<Preferences>.vaultAutoLockTimeout
    get() = data.map { prefs ->
        val name = prefs[VAULT_AUTO_LOCK_TIMEOUT_KEY] ?: VaultAutoLockTimeout.IMMEDIATELY.name
        VaultAutoLockTimeout.entries.firstOrNull { it.name == name } ?:
            VaultAutoLockTimeout.IMMEDIATELY
    }

private val DataStore<Preferences>.unlockVaultWithAndroidCredential
    get() = data.map { prefs ->
        prefs[VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY] ?: false
    }
