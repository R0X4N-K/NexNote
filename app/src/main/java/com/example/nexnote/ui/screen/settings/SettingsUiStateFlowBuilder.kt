package com.example.nexnote.ui.screen.settings

import com.example.nexnote.domain.model.AccentColor
import com.example.nexnote.domain.model.FontScale
import com.example.nexnote.domain.model.NoteCardStyle
import com.example.nexnote.domain.model.ThemeMode
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
    val isLeftHanded: Flow<Boolean>,
    val accentColor: Flow<AccentColor>,
    val noteCardStyle: Flow<NoteCardStyle>
)

private data class SettingsDisplayPreferences(
    val themeMode: ThemeMode,
    val fontScale: FontScale,
    val timezoneId: String
)

private data class SettingsInteractionPreferences(
    val isLeftHanded: Boolean,
    val accentColor: AccentColor,
    val noteCardStyle: NoteCardStyle
)

internal fun buildSettingsUiStateFlow(
    flows: SettingsUiStateFlows,
    scope: CoroutineScope
): StateFlow<SettingsUiState> {
    return combine(
        combine(flows.themeMode, flows.fontScale, flows.timezoneId, ::SettingsDisplayPreferences),
        combine(
            flows.isLeftHanded,
            flows.accentColor,
            flows.noteCardStyle,
            ::SettingsInteractionPreferences
        )
    ) { display, interaction ->
        buildSettingsUiState(display, interaction)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )
}

private fun buildSettingsUiState(
    display: SettingsDisplayPreferences,
    interaction: SettingsInteractionPreferences
): SettingsUiState {
    return SettingsUiState(
        themeMode = display.themeMode,
        fontScale = display.fontScale,
        timezoneId = display.timezoneId,
        availableTimezones = TimeZone.getAvailableIDs().toList().sorted(),
        isLeftHanded = interaction.isLeftHanded,
        accentColor = interaction.accentColor,
        noteCardStyle = interaction.noteCardStyle
    )
}
