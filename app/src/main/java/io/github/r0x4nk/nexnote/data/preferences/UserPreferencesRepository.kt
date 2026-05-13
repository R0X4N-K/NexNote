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
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private val Flow<Preferences>.safe: Flow<Preferences>
    get() = catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

private inline fun <reified E : Enum<E>> Flow<Preferences>.observeEnum(
    key: Preferences.Key<String>,
    default: E
): Flow<E> = safe.map { prefs ->
    val name = prefs[key] ?: default.name
    enumValues<E>().firstOrNull { it.name == name } ?: default
}

class UserPreferencesRepository(private val context: Context) : IUserPreferencesRepository {

    companion object {
        val THEME_MODE_KEY       = stringPreferencesKey("theme_mode")
        val FONT_SCALE_KEY       = stringPreferencesKey("font_scale")
        val TIMEZONE_KEY         = stringPreferencesKey("timezone_id")
        val LEFT_HANDED_KEY      = booleanPreferencesKey("is_left_handed")
        val ACCENT_COLOR_KEY     = stringPreferencesKey("accent_color")
        val NOTE_CARD_STYLE_KEY  = stringPreferencesKey("note_card_style")
        // EDITOR_BACKGROUND_KEY removed — per-note color replaced the global background setting.
    }

    override val themeMode: Flow<ThemeMode> =
        context.dataStore.data.observeEnum(THEME_MODE_KEY, ThemeMode.SYSTEM)

    override val fontScale: Flow<FontScale> =
        context.dataStore.data.observeEnum(FONT_SCALE_KEY, FontScale.NORMAL)

    override val timezoneId: Flow<String> = context.dataStore.data
        .safe
        .map { prefs -> prefs[TIMEZONE_KEY] ?: "" }

    override val isLeftHanded: Flow<Boolean> = context.dataStore.data
        .safe
        .map { prefs -> prefs[LEFT_HANDED_KEY] ?: false }

    override val accentColor: Flow<AccentColor> =
        context.dataStore.data.observeEnum(ACCENT_COLOR_KEY, AccentColor.VIOLET)

    override val noteCardStyle: Flow<NoteCardStyle> =
        context.dataStore.data.observeEnum(NOTE_CARD_STYLE_KEY, NoteCardStyle.TITLE_AND_PREVIEW)

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun setFontScale(scale: FontScale) {
        context.dataStore.edit { prefs -> prefs[FONT_SCALE_KEY] = scale.name }
    }

    override suspend fun setTimezoneId(id: String) {
        context.dataStore.edit { prefs -> prefs[TIMEZONE_KEY] = id }
    }

    override suspend fun setLeftHanded(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[LEFT_HANDED_KEY] = value }
    }

    override suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { prefs -> prefs[ACCENT_COLOR_KEY] = color.name }
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        context.dataStore.edit { prefs -> prefs[NOTE_CARD_STYLE_KEY] = style.name }
    }

}
