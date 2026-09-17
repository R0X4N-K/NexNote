package io.github.r0x4nk.nexnote.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.AppFont
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private val Flow<Preferences>.safe: Flow<Preferences>
    get() = catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

private inline fun <reified E : Enum<E>> Flow<Preferences>.observeEnum(
    key: Preferences.Key<String>,
    default: E,
    legacyNames: Map<String, String> = emptyMap()
): Flow<E> = safe.map { prefs ->
    val stored = prefs[key] ?: default.name
    val name = legacyNames[stored] ?: stored
    enumValues<E>().firstOrNull { it.name == name } ?: default
}

class UserPreferencesRepository(private val context: Context) : IUserPreferencesRepository {

    companion object {
        val THEME_MODE_KEY       = stringPreferencesKey("theme_mode")
        val APP_FONT_KEY         = stringPreferencesKey("app_font")
        val FONT_SCALE_KEY       = stringPreferencesKey("font_scale")
        val TIMEZONE_KEY         = stringPreferencesKey("timezone_id")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR_KEY     = stringPreferencesKey("accent_color")
        val NOTE_CARD_STYLE_KEY  = stringPreferencesKey("note_card_style")
        val TABLE_LAYOUT_MODE_KEY = stringPreferencesKey("table_layout_mode")
        val VAULT_RECENT_PREVIEWS_PROTECTED_KEY =
            booleanPreferencesKey("vault_recent_previews_protected")
        val VAULT_LOCK_ON_BACKGROUND_KEY =
            booleanPreferencesKey("vault_lock_on_background")
        val VAULT_AUTO_LOCK_TIMEOUT_KEY =
            stringPreferencesKey("vault_auto_lock_timeout")
        val VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY =
            booleanPreferencesKey("vault_android_credential_unlock_enabled")
        val PREDEFINED_TEMPLATES_SEEDED_KEY =
            booleanPreferencesKey("predefined_templates_seeded")
        // EDITOR_BACKGROUND_KEY removed — per-note color replaced the global background setting.
    }

    override val themeMode: Flow<ThemeMode> =
        context.dataStore.data.observeEnum(THEME_MODE_KEY, ThemeMode.SYSTEM)

    override val appFont: Flow<AppFont> =
        context.dataStore.data.observeEnum(APP_FONT_KEY, AppFont.SYSTEM)

    override val fontScale: Flow<FontScale> =
        context.dataStore.data.observeEnum(FONT_SCALE_KEY, FontScale.NORMAL)

    override val timezoneId: Flow<String> = context.dataStore.data
        .safe
        .map { prefs -> prefs[TIMEZONE_KEY] ?: "" }

    override val dynamicColor: Flow<Boolean> = context.dataStore.data.safe.map { it[DYNAMIC_COLOR_KEY] ?: false }

    override val accentColor: Flow<AccentColor> =
        context.dataStore.data.observeEnum(ACCENT_COLOR_KEY, AccentColor.VIOLET)

    override val noteCardStyle: Flow<NoteCardStyle> =
        context.dataStore.data.observeEnum(
            key = NOTE_CARD_STYLE_KEY,
            default = NoteCardStyle.TITLE_AND_PREVIEW,
            // "TITLE_DATE" was the legacy name of the information style.
            legacyNames = mapOf("TITLE_DATE" to NoteCardStyle.TITLE_INFORMATION.name)
        )

    override val tableLayoutMode: Flow<TableLayoutMode> =
        context.dataStore.data.observeEnum(TABLE_LAYOUT_MODE_KEY, TableLayoutMode.FIT_SCREEN)

    override val protectVaultRecentPreviews: Flow<Boolean> = context.dataStore.data
        .safe
        .map { prefs -> prefs[VAULT_RECENT_PREVIEWS_PROTECTED_KEY] ?: true }

    override val lockVaultOnBackground: Flow<Boolean> = context.dataStore.data
        .safe
        .map { prefs -> prefs[VAULT_LOCK_ON_BACKGROUND_KEY] ?: true }

    override val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout> =
        context.dataStore.data.observeEnum(
            VAULT_AUTO_LOCK_TIMEOUT_KEY,
            VaultAutoLockTimeout.IMMEDIATELY
        )

    override val unlockVaultWithAndroidCredential: Flow<Boolean> = context.dataStore.data
        .safe
        .map { prefs -> prefs[VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY] ?: false }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { prefs -> prefs[APP_FONT_KEY] = font.name }
    }

    override suspend fun setFontScale(scale: FontScale) {
        context.dataStore.edit { prefs -> prefs[FONT_SCALE_KEY] = scale.name }
    }

    override suspend fun setTimezoneId(id: String) {
        context.dataStore.edit { prefs -> prefs[TIMEZONE_KEY] = id }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    override suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { prefs -> prefs[ACCENT_COLOR_KEY] = color.name }
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        context.dataStore.edit { prefs -> prefs[NOTE_CARD_STYLE_KEY] = style.name }
    }

    override suspend fun setTableLayoutMode(mode: TableLayoutMode) {
        context.dataStore.edit { prefs -> prefs[TABLE_LAYOUT_MODE_KEY] = mode.name }
    }

    override suspend fun setProtectVaultRecentPreviews(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[VAULT_RECENT_PREVIEWS_PROTECTED_KEY] = value }
    }

    override suspend fun setLockVaultOnBackground(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[VAULT_LOCK_ON_BACKGROUND_KEY] = value }
    }

    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) {
        context.dataStore.edit { prefs -> prefs[VAULT_AUTO_LOCK_TIMEOUT_KEY] = timeout.name }
    }

    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[VAULT_ANDROID_CREDENTIAL_UNLOCK_ENABLED_KEY] = value
        }
    }

    suspend fun hasSeededPredefinedTemplates(): Boolean =
        context.dataStore.data.safe.first()[PREDEFINED_TEMPLATES_SEEDED_KEY] ?: false

    suspend fun setPredefinedTemplatesSeeded() {
        context.dataStore.edit { prefs ->
            prefs[PREDEFINED_TEMPLATES_SEEDED_KEY] = true
        }
    }

}
