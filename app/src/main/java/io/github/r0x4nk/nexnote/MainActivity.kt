package io.github.r0x4nk.nexnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.ui.navigation.AppNavigation
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = (application as NexNoteApp).useCases.preferences

        setContent {
            val themeMode by preferences.observeThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val fontScale by preferences.observeFontScale().collectAsStateWithLifecycle(FontScale.NORMAL)
            val isLeftHanded by preferences.observeLeftHanded().collectAsStateWithLifecycle(false)
            val accentColor by preferences.observeAccentColor().collectAsStateWithLifecycle(AccentColor.VIOLET)

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT     -> false
                ThemeMode.DARK      -> true
                ThemeMode.SYSTEM    -> isSystemInDarkTheme()
                ThemeMode.TRUE_DARK -> true
            }
            val trueDark = themeMode == ThemeMode.TRUE_DARK

            NexNoteTheme(
                darkTheme   = darkTheme,
                trueDark    = trueDark,
                fontScale   = fontScale.multiplier,
                accentColor = accentColor
            ) {
                AppNavigation(isLeftHanded = isLeftHanded)
            }
        }
    }
}
