package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import io.github.r0x4nk.nexnote.domain.model.AccentColor

/** Seeds preserve the identity of existing preferences; every role comes from one scheme. */
internal fun AccentColor.seedColor(): Color = when (this) {
    AccentColor.VIOLET -> Color(0xFF5654D4)
    AccentColor.BLUE -> Color(0xFF0061A6)
    AccentColor.GREEN -> Color(0xFF3D6B1F)
    AccentColor.ORANGE -> Color(0xFF9B4400)
    AccentColor.RED -> Color(0xFFBA1A1A)
    AccentColor.TEAL -> Color(0xFF006A6A)
    AccentColor.SAGE -> Color(0xFFBBE1C3)
    AccentColor.ROSE -> Color(0xFF984061)
    AccentColor.AMBER -> Color(0xFF7B5800)
}

internal fun buildColorScheme(darkTheme: Boolean, trueDark: Boolean, accent: AccentColor): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(accent.seedColor().toArgb()), darkTheme, 0.0)
    val roles = MaterialDynamicColors()
    val colors = lightColorScheme(
        primary = Color(roles.primary().getArgb(scheme)),
        onPrimary = Color(roles.onPrimary().getArgb(scheme)),
        primaryContainer = Color(roles.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = Color(roles.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = Color(roles.inversePrimary().getArgb(scheme)),
        secondary = Color(roles.secondary().getArgb(scheme)),
        onSecondary = Color(roles.onSecondary().getArgb(scheme)),
        secondaryContainer = Color(roles.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = Color(roles.onSecondaryContainer().getArgb(scheme)),
        tertiary = Color(roles.tertiary().getArgb(scheme)),
        onTertiary = Color(roles.onTertiary().getArgb(scheme)),
        tertiaryContainer = Color(roles.tertiaryContainer().getArgb(scheme)),
        onTertiaryContainer = Color(roles.onTertiaryContainer().getArgb(scheme)),
        primaryFixed = Color(roles.primaryFixed().getArgb(scheme)),
        primaryFixedDim = Color(roles.primaryFixedDim().getArgb(scheme)),
        onPrimaryFixed = Color(roles.onPrimaryFixed().getArgb(scheme)),
        onPrimaryFixedVariant = Color(roles.onPrimaryFixedVariant().getArgb(scheme)),
        secondaryFixed = Color(roles.secondaryFixed().getArgb(scheme)),
        secondaryFixedDim = Color(roles.secondaryFixedDim().getArgb(scheme)),
        onSecondaryFixed = Color(roles.onSecondaryFixed().getArgb(scheme)),
        onSecondaryFixedVariant = Color(roles.onSecondaryFixedVariant().getArgb(scheme)),
        tertiaryFixed = Color(roles.tertiaryFixed().getArgb(scheme)),
        tertiaryFixedDim = Color(roles.tertiaryFixedDim().getArgb(scheme)),
        onTertiaryFixed = Color(roles.onTertiaryFixed().getArgb(scheme)),
        onTertiaryFixedVariant = Color(roles.onTertiaryFixedVariant().getArgb(scheme)),
        background = Color(roles.background().getArgb(scheme)),
        onBackground = Color(roles.onBackground().getArgb(scheme)),
        surface = Color(roles.surface().getArgb(scheme)),
        onSurface = Color(roles.onSurface().getArgb(scheme)),
        surfaceVariant = Color(roles.surfaceVariant().getArgb(scheme)),
        onSurfaceVariant = Color(roles.onSurfaceVariant().getArgb(scheme)),
        surfaceTint = Color(roles.surfaceTint().getArgb(scheme)),
        inverseSurface = Color(roles.inverseSurface().getArgb(scheme)),
        inverseOnSurface = Color(roles.inverseOnSurface().getArgb(scheme)),
        error = Color(roles.error().getArgb(scheme)),
        onError = Color(roles.onError().getArgb(scheme)),
        errorContainer = Color(roles.errorContainer().getArgb(scheme)),
        onErrorContainer = Color(roles.onErrorContainer().getArgb(scheme)),
        outline = Color(roles.outline().getArgb(scheme)),
        outlineVariant = Color(roles.outlineVariant().getArgb(scheme)),
        scrim = Color(roles.scrim().getArgb(scheme)),
        surfaceBright = Color(roles.surfaceBright().getArgb(scheme)),
        surfaceDim = Color(roles.surfaceDim().getArgb(scheme)),
        surfaceContainer = Color(roles.surfaceContainer().getArgb(scheme)),
        surfaceContainerHigh = Color(roles.surfaceContainerHigh().getArgb(scheme)),
        surfaceContainerHighest = Color(roles.surfaceContainerHighest().getArgb(scheme)),
        surfaceContainerLow = Color(roles.surfaceContainerLow().getArgb(scheme)),
        surfaceContainerLowest = Color(roles.surfaceContainerLowest().getArgb(scheme))
    )
    return if (darkTheme && trueDark) colors.toOledScheme() else colors
}

/** Optional OLED customization, not the Material dark baseline. Keep tonal container steps. */
internal fun ColorScheme.toOledScheme(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black
)
