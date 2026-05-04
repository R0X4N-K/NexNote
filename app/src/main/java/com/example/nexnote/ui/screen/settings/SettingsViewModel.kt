package com.example.nexnote.ui.screen.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nexnote.NexNoteApp
import com.example.nexnote.domain.model.AccentColor
import com.example.nexnote.domain.model.FontScale
import com.example.nexnote.domain.model.NoteCardStyle
import com.example.nexnote.domain.model.ThemeMode
import com.example.nexnote.domain.usecase.ObserveAccentColorUseCase
import com.example.nexnote.domain.usecase.ObserveFontScaleUseCase
import com.example.nexnote.domain.usecase.ObserveLeftHandedUseCase
import com.example.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import com.example.nexnote.domain.usecase.ObserveThemeModeUseCase
import com.example.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import com.example.nexnote.domain.usecase.SetAccentColorUseCase
import com.example.nexnote.domain.usecase.SetFontScaleUseCase
import com.example.nexnote.domain.usecase.SetLeftHandedUseCase
import com.example.nexnote.domain.usecase.SetNoteCardStyleUseCase
import com.example.nexnote.domain.usecase.SetThemeModeUseCase
import com.example.nexnote.domain.usecase.SetTimezoneIdUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val themeMode:          ThemeMode     = ThemeMode.SYSTEM,
    val fontScale:          FontScale     = FontScale.NORMAL,
    val timezoneId:         String        = "",
    val availableTimezones: List<String>  = emptyList(),
    val isLeftHanded:       Boolean       = false,
    val accentColor:        AccentColor   = AccentColor.VIOLET,
    val noteCardStyle:      NoteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW
)

class SettingsViewModel(
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val observeFontScale: ObserveFontScaleUseCase,
    private val observeTimezoneId: ObserveTimezoneIdUseCase,
    private val observeLeftHanded: ObserveLeftHandedUseCase,
    private val observeAccentColor: ObserveAccentColorUseCase,
    private val observeNoteCardStyle: ObserveNoteCardStyleUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setFontScaleUseCase: SetFontScaleUseCase,
    private val setTimezoneIdUseCase: SetTimezoneIdUseCase,
    private val setLeftHandedUseCase: SetLeftHandedUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    private val setNoteCardStyleUseCase: SetNoteCardStyleUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = buildSettingsUiStateFlow(
        flows = SettingsUiStateFlows(
            themeMode = observeThemeMode(),
            fontScale = observeFontScale(),
            timezoneId = observeTimezoneId(),
            isLeftHanded = observeLeftHanded(),
            accentColor = observeAccentColor(),
            noteCardStyle = observeNoteCardStyle()
        ),
        scope = viewModelScope
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { setThemeModeUseCase(mode) }
    }

    fun setFontScale(scale: FontScale) {
        viewModelScope.launch { setFontScaleUseCase(scale) }
    }

    fun setTimezoneId(id: String) {
        viewModelScope.launch { setTimezoneIdUseCase(id) }
    }

    fun setLeftHanded(value: Boolean) {
        viewModelScope.launch { setLeftHandedUseCase(value) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { setAccentColorUseCase(color) }
    }

    fun setNoteCardStyle(style: NoteCardStyle) {
        viewModelScope.launch { setNoteCardStyleUseCase(style) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val preferences = app.useCases.preferences
                SettingsViewModel(
                    observeThemeMode = preferences.observeThemeMode,
                    observeFontScale = preferences.observeFontScale,
                    observeTimezoneId = preferences.observeTimezoneId,
                    observeLeftHanded = preferences.observeLeftHanded,
                    observeAccentColor = preferences.observeAccentColor,
                    observeNoteCardStyle = preferences.observeNoteCardStyle,
                    setThemeModeUseCase = preferences.setThemeMode,
                    setFontScaleUseCase = preferences.setFontScale,
                    setTimezoneIdUseCase = preferences.setTimezoneId,
                    setLeftHandedUseCase = preferences.setLeftHanded,
                    setAccentColorUseCase = preferences.setAccentColor,
                    setNoteCardStyleUseCase = preferences.setNoteCardStyle
                )
            }
        }
    }
}
