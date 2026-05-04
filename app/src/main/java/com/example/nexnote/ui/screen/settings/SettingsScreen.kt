package com.example.nexnote.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        onThemeModeChange = viewModel::setThemeMode,
        onAccentColorChange = viewModel::setAccentColor,
        onFontScaleChange = viewModel::setFontScale,
        onNoteCardStyleChange = viewModel::setNoteCardStyle,
        onLeftHandedChange = viewModel::setLeftHanded,
        onTimezoneChange = viewModel::setTimezoneId
    )
}
