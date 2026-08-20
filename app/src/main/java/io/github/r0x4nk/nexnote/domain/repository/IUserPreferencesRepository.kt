package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import kotlinx.coroutines.flow.Flow

interface IUserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val fontScale: Flow<FontScale>
    val timezoneId: Flow<String>
    val accentColor: Flow<AccentColor>
    val noteCardStyle: Flow<NoteCardStyle>
    val tableLayoutMode: Flow<TableLayoutMode>
    val protectVaultRecentPreviews: Flow<Boolean>
    val lockVaultOnBackground: Flow<Boolean>
    val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout>
    val unlockVaultWithAndroidCredential: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setFontScale(scale: FontScale)
    suspend fun setTimezoneId(id: String)
    suspend fun setAccentColor(color: AccentColor)
    suspend fun setNoteCardStyle(style: NoteCardStyle)
    suspend fun setTableLayoutMode(mode: TableLayoutMode)
    suspend fun setProtectVaultRecentPreviews(value: Boolean)
    suspend fun setLockVaultOnBackground(value: Boolean)
    suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout)
    suspend fun setUnlockVaultWithAndroidCredential(value: Boolean)
}
