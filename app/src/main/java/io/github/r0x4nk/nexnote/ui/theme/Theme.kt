package io.github.r0x4nk.nexnote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import io.github.r0x4nk.nexnote.domain.model.AccentColor

internal val LocalNexNoteDarkTheme = staticCompositionLocalOf { false }
internal val LocalNexNoteOledTheme = staticCompositionLocalOf { false }

/**
 * Main NexNote theme.
 *
 * [darkTheme]   - controls the base color scheme; follows system by default.
 * [trueDark]    - when true and dark, replaces surfaces with pure black (OLED).
 * [fontScale]   - sp multiplier applied to the entire typography (0.85 / 1.0 / 1.15).
 * [fontFamily]  - typeface applied to the entire typography.
 * [accentColor] - seed for all color families when device colors are disabled.
 * [dynamicColor] - opt-in Android 12+ wallpaper palette; manual accent is the fallback.
 *
 * The selected accent acts as the app key color so existing color preferences
 * keep working across light, dark, system, and true-dark modes.
 */
@Composable
fun NexNoteTheme(
    darkTheme:   Boolean     = isSystemInDarkTheme(),
    trueDark:    Boolean     = false,
    fontScale:   Float       = 1.0f,
    fontFamily:  FontFamily  = FontFamily.Default,
    accentColor: AccentColor = AccentColor.VIOLET,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colorScheme = remember(darkTheme, trueDark, accentColor, dynamicColor, context, configuration) {
        val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            buildColorScheme(darkTheme, false, accentColor)
        }
        if (darkTheme && trueDark) base.toOledScheme() else base
    }
    val typography = remember(fontScale, fontFamily) {
        buildNexNoteTypography(fontScale, fontFamily)
    }

    CompositionLocalProvider(
        LocalNexNoteDarkTheme provides darkTheme,
        LocalNexNoteOledTheme provides (darkTheme && trueDark)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = typography,
            shapes      = NexNoteShapes,
            content     = content
        )
    }
}
