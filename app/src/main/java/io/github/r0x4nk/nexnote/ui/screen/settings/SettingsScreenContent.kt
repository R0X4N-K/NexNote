package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = nexTopAppBarColors()
            )
        }
    ) { innerPadding ->
        SettingsList(
            uiState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        appearanceSection(uiState.themeMode, onThemeModeChange)
        accentColorSection(uiState.accentColor, onAccentColorChange)
        textSection(uiState.fontScale, onFontScaleChange)
        noteAppearanceSection(uiState.noteCardStyle, onNoteCardStyleChange)
        accessibilitySection(uiState.isLeftHanded, onLeftHandedChange)
        timezoneSection(uiState, onTimezoneChange)
    }
}

private fun LazyListScope.appearanceSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Appearance")
            Spacer(Modifier.height(10.dp))
            ThemeModePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.accentColorSection(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Accent color")
            Spacer(Modifier.height(14.dp))
            AccentColorPicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.textSection(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Text")
            Spacer(Modifier.height(10.dp))
            FontScalePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.noteAppearanceSection(
    selected: NoteCardStyle,
    onSelect: (NoteCardStyle) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Note appearance")
            Spacer(Modifier.height(10.dp))
            NoteCardStylePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.accessibilitySection(
    isLeftHanded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Accessibility")
            Spacer(Modifier.height(10.dp))
            LeftHandedToggle(
                isLeftHanded = isLeftHanded,
                onToggle = onToggle
            )
        }
    }
}

private fun LazyListScope.timezoneSection(
    uiState: SettingsUiState,
    onSelect: (String) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Timezone")
            Spacer(Modifier.height(10.dp))
            TimezoneDropdown(
                selectedId = uiState.timezoneId,
                availableTimezones = uiState.availableTimezones,
                onSelect = onSelect
            )
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun SettingsSectionSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}
