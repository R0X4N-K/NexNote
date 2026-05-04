package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onFontScaleChange: (FontScale) -> Unit,
    onNoteCardStyleChange: (NoteCardStyle) -> Unit,
    onLeftHandedChange: (Boolean) -> Unit,
    onTimezoneChange: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        SettingsList(
            uiState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            onThemeModeChange = onThemeModeChange,
            onAccentColorChange = onAccentColorChange,
            onFontScaleChange = onFontScaleChange,
            onNoteCardStyleChange = onNoteCardStyleChange,
            onLeftHandedChange = onLeftHandedChange,
            onTimezoneChange = onTimezoneChange
        )
    }
}

@Composable
private fun SettingsList(
    uiState: SettingsUiState,
    modifier: Modifier,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onFontScaleChange: (FontScale) -> Unit,
    onNoteCardStyleChange: (NoteCardStyle) -> Unit,
    onLeftHandedChange: (Boolean) -> Unit,
    onTimezoneChange: (String) -> Unit
) {
    LazyColumn(modifier = modifier) {
        appearanceSection(uiState.themeMode, onThemeModeChange)
        settingsDivider()
        accentColorSection(uiState.accentColor, onAccentColorChange)
        settingsDivider()
        textSection(uiState.fontScale, onFontScaleChange)
        settingsDivider()
        noteAppearanceSection(uiState.noteCardStyle, onNoteCardStyleChange)
        settingsDivider()
        accessibilitySection(uiState.isLeftHanded, onLeftHandedChange)
        settingsDivider()
        timezoneSection(uiState, onTimezoneChange)
    }
}

private fun LazyListScope.appearanceSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    item {
        Spacer(Modifier.height(16.dp))
        SettingsSectionHeader("Appearance")
        Spacer(Modifier.height(8.dp))
        ThemeModePicker(
            selected = selected,
            onSelect = onSelect
        )
    }
}

private fun LazyListScope.accentColorSection(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    item {
        Spacer(Modifier.height(16.dp))
        SettingsSectionHeader("Accent color")
        Spacer(Modifier.height(12.dp))
        AccentColorPicker(
            selected = selected,
            onSelect = onSelect
        )
    }
}

private fun LazyListScope.textSection(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    item {
        Spacer(Modifier.height(16.dp))
        SettingsSectionHeader("Text")
        Spacer(Modifier.height(8.dp))
        FontScalePicker(
            selected = selected,
            onSelect = onSelect
        )
    }
}

private fun LazyListScope.noteAppearanceSection(
    selected: NoteCardStyle,
    onSelect: (NoteCardStyle) -> Unit
) {
    item {
        Spacer(Modifier.height(16.dp))
        SettingsSectionHeader("Note appearance")
        Spacer(Modifier.height(8.dp))
        NoteCardStylePicker(
            selected = selected,
            onSelect = onSelect
        )
    }
}

private fun LazyListScope.accessibilitySection(
    isLeftHanded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    item {
        Spacer(Modifier.height(16.dp))
        SettingsSectionHeader("Accessibility")
        Spacer(Modifier.height(8.dp))
        LeftHandedToggle(
            isLeftHanded = isLeftHanded,
            onToggle = onToggle
        )
    }
}

private fun LazyListScope.timezoneSection(
    uiState: SettingsUiState,
    onSelect: (String) -> Unit
) {
    item {
        Spacer(Modifier.height(16.dp))
        SettingsSectionHeader("Timezone")
        Spacer(Modifier.height(8.dp))
        TimezoneDropdown(
            selectedId = uiState.timezoneId,
            availableTimezones = uiState.availableTimezones,
            onSelect = onSelect
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun LazyListScope.settingsDivider() {
    item {
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
    }
}
