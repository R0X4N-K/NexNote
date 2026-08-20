package io.github.r0x4nk.nexnote.ui.screen.settings

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.TimeZone

internal data class SettingsUiStateFlows(
    val themeMode: Flow<ThemeMode>,
    val fontScale: Flow<FontScale>,
    val timezoneId: Flow<String>,
    val accentColor: Flow<AccentColor>,
    val noteCardStyle: Flow<NoteCardStyle>,
    val tableLayoutMode: Flow<TableLayoutMode>,
    val vaultState: Flow<VaultState>,
    val protectVaultRecentPreviews: Flow<Boolean>,
    val lockVaultOnBackground: Flow<Boolean>,
    val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout>,
    val unlockVaultWithAndroidCredential: Flow<Boolean>
)

private data class SettingsDisplayPreferences(
    val themeMode: ThemeMode,
    val fontScale: FontScale,
    val timezoneId: String
)

private data class SettingsAppearancePreferences(
    val accentColor: AccentColor,
    val noteCardStyle: NoteCardStyle,
    val tableLayoutMode: TableLayoutMode
)

private data class SettingsVaultPreferences(
    val vaultState: VaultState,
    val protectRecentPreviews: Boolean,
    val lockOnBackground: Boolean,
    val autoLockTimeout: VaultAutoLockTimeout,
    val unlockWithAndroidCredential: Boolean
)

internal fun buildSettingsUiStateFlow(
    flows: SettingsUiStateFlows,
    scope: CoroutineScope
): StateFlow<SettingsUiState> {
    return combine(
        combine(flows.themeMode, flows.fontScale, flows.timezoneId, ::SettingsDisplayPreferences),
        combine(
            flows.accentColor,
            flows.noteCardStyle,
            flows.tableLayoutMode,
            ::SettingsAppearancePreferences
        ),
        combine(
            flows.vaultState,
            flows.protectVaultRecentPreviews,
            flows.lockVaultOnBackground,
            flows.vaultAutoLockTimeout,
            flows.unlockVaultWithAndroidCredential,
            ::SettingsVaultPreferences
        )
    ) { display, appearance, vault ->
        buildSettingsUiState(display, appearance, vault)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )
}

private fun buildSettingsUiState(
    display: SettingsDisplayPreferences,
    appearance: SettingsAppearancePreferences,
    vault: SettingsVaultPreferences
): SettingsUiState {
    return SettingsUiState(
        themeMode = display.themeMode,
        fontScale = display.fontScale,
        timezoneId = display.timezoneId,
        availableTimezones = TimeZone.getAvailableIDs().toList().sorted(),
        accentColor = appearance.accentColor,
        noteCardStyle = appearance.noteCardStyle,
        tableLayoutMode = appearance.tableLayoutMode,
        vaultState = vault.vaultState,
        protectVaultRecentPreviews = vault.protectRecentPreviews,
        lockVaultOnBackground = vault.lockOnBackground,
        vaultAutoLockTimeout = vault.autoLockTimeout,
        unlockVaultWithAndroidCredential = vault.unlockWithAndroidCredential
    )
}
