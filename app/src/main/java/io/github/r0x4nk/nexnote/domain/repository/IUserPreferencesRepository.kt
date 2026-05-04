package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface IUserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val fontScale: Flow<FontScale>
    val timezoneId: Flow<String>
    val isLeftHanded: Flow<Boolean>
    val accentColor: Flow<AccentColor>
    val noteCardStyle: Flow<NoteCardStyle>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setFontScale(scale: FontScale)
    suspend fun setTimezoneId(id: String)
    suspend fun setLeftHanded(value: Boolean)
    suspend fun setAccentColor(color: AccentColor)
    suspend fun setNoteCardStyle(style: NoteCardStyle)
}
