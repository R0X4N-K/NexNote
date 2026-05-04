package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.r0x4nk.nexnote.domain.model.AccentColor

private val LightColorScheme = lightColorScheme(
    primary = NexPrimary,
    onPrimary = NexOnPrimary,
    primaryContainer = NexPrimaryContainer,
    onPrimaryContainer = NexOnPrimaryContainer,
    secondary = NexSecondary,
    onSecondary = NexOnSecondary,
    secondaryContainer = NexSecondaryContainer,
    onSecondaryContainer = NexOnSecondaryContainer,
    tertiary = NexTertiary,
    onTertiary = NexOnTertiary,
    tertiaryContainer = NexTertiaryContainer,
    onTertiaryContainer = NexOnTertiaryContainer,
    background = NexBackground,
    onBackground = NexOnSurface,
    surface = NexSurface,
    onSurface = NexOnSurface,
    surfaceVariant = NexSurfaceVariant,
    onSurfaceVariant = NexOnSurfaceVariant,
    surfaceContainerLowest = NexSurfaceContainerLowest,
    surfaceContainerLow = NexSurfaceContainerLow,
    surfaceContainer = NexSurfaceContainer,
    surfaceContainerHigh = NexSurfaceContainerHigh,
    surfaceContainerHighest = NexSurfaceContainerHighest,
    surfaceDim = NexSurfaceDim,
    surfaceBright = NexSurfaceBright,
    inverseSurface = NexInverseSurface,
    inverseOnSurface = NexInverseOnSurface,
    inversePrimary = NexInversePrimary,
    outline = NexOutline,
    outlineVariant = NexOutlineVariant,
    error = NexError,
    onError = NexOnError,
    errorContainer = NexErrorContainer,
    onErrorContainer = NexOnErrorContainer,
    surfaceTint = NexPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = NexPrimaryDark,
    onPrimary = NexOnPrimaryDark,
    primaryContainer = NexPrimaryContainerDark,
    onPrimaryContainer = NexOnPrimaryContainerDark,
    secondary = NexSecondaryDark,
    onSecondary = NexOnSecondaryDark,
    secondaryContainer = NexSecondaryContainerDark,
    onSecondaryContainer = NexOnSecondaryContainerDark,
    tertiary = NexTertiaryDark,
    onTertiary = NexOnTertiaryDark,
    tertiaryContainer = NexTertiaryContainerDark,
    onTertiaryContainer = NexOnTertiaryContainerDark,
    background = NexBackgroundDark,
    onBackground = NexOnSurfaceDark,
    surface = NexSurfaceDark,
    onSurface = NexOnSurfaceDark,
    onSurfaceVariant = NexOnSurfaceVariantDark,
    surfaceVariant = NexSurfaceVariantDark,
    surfaceContainerLowest = NexSurfaceContainerLowestDark,
    surfaceContainerLow = NexSurfaceContainerLowDark,
    surfaceContainer = NexSurfaceContainerDark,
    surfaceContainerHigh = NexSurfaceContainerHighDark,
    surfaceContainerHighest = NexSurfaceContainerHighestDark,
    surfaceDim = NexSurfaceDimDark,
    surfaceBright = NexSurfaceBrightDark,
    inverseSurface = NexInverseSurfaceDark,
    inverseOnSurface = NexInverseOnSurfaceDark,
    inversePrimary = NexInversePrimaryDark,
    outline = NexOutlineDark,
    outlineVariant = NexOutlineVariantDark,
    error = NexErrorDark,
    onError = NexOnErrorDark,
    errorContainer = NexErrorContainerDark,
    onErrorContainer = NexOnErrorContainerDark,
    surfaceTint = NexPrimaryDark
)

/**
 * Holds the four primary-family colors for one accent and one light/dark variant.
 */
private data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

private data class AccentPaletteVariants(
    val light: AccentPalette,
    val dark: AccentPalette
)

private val AccentPalettesByColor = mapOf(
    AccentColor.VIOLET to AccentPaletteVariants(
        light = AccentPalette(NexPrimary, NexOnPrimary, NexPrimaryContainer, NexOnPrimaryContainer),
        dark = AccentPalette(NexPrimaryDark, NexOnPrimaryDark, NexPrimaryContainerDark, NexOnPrimaryContainerDark)
    ),
    AccentColor.BLUE to AccentPaletteVariants(
        light = AccentPalette(NexBluePrimary, NexBlueOnPrimary, NexBluePrimaryContainer, NexBlueOnPrimaryContainer),
        dark = AccentPalette(NexBluePrimaryDark, NexBlueOnPrimaryDark, NexBluePrimaryContainerDark, NexBlueOnPrimaryContainerDark)
    ),
    AccentColor.GREEN to AccentPaletteVariants(
        light = AccentPalette(NexGreenPrimary, NexGreenOnPrimary, NexGreenPrimaryContainer, NexGreenOnPrimaryContainer),
        dark = AccentPalette(NexGreenPrimaryDark, NexGreenOnPrimaryDark, NexGreenPrimaryContainerDark, NexGreenOnPrimaryContainerDark)
    ),
    AccentColor.ORANGE to AccentPaletteVariants(
        light = AccentPalette(NexOrangePrimary, NexOrangeOnPrimary, NexOrangePrimaryContainer, NexOrangeOnPrimaryContainer),
        dark = AccentPalette(NexOrangePrimaryDark, NexOrangeOnPrimaryDark, NexOrangePrimaryContainerDark, NexOrangeOnPrimaryContainerDark)
    ),
    AccentColor.RED to AccentPaletteVariants(
        light = AccentPalette(NexRedPrimary, NexRedOnPrimary, NexRedPrimaryContainer, NexRedOnPrimaryContainer),
        dark = AccentPalette(NexRedPrimaryDark, NexRedOnPrimaryDark, NexRedPrimaryContainerDark, NexRedOnPrimaryContainerDark)
    ),
    AccentColor.TEAL to AccentPaletteVariants(
        light = AccentPalette(NexTealPrimary, NexTealOnPrimary, NexTealPrimaryContainer, NexTealOnPrimaryContainer),
        dark = AccentPalette(NexTealPrimaryDark, NexTealOnPrimaryDark, NexTealPrimaryContainerDark, NexTealOnPrimaryContainerDark)
    )
)

/** Returns the accent palette for [accent] in light or dark mode. */
private fun accentPalette(dark: Boolean, accent: AccentColor): AccentPalette {
    val variants = AccentPalettesByColor.getValue(accent)
    return if (dark) variants.dark else variants.light
}

/**
 * Builds the final [ColorScheme] combining dark/light base, true-dark OLED surfaces,
 * and the selected accent color.
 */
private fun buildColorScheme(
    darkTheme: Boolean,
    trueDark: Boolean,
    accent: AccentColor
): ColorScheme {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme

    // Apply true-dark (OLED black) surface overrides when requested.
    val surfaceAdjusted = if (trueDark && darkTheme) base.copy(
        background = NexTrueDarkBackground,
        surface = NexTrueDarkSurface,
        surfaceVariant = NexTrueDarkSurfaceVariant,
        surfaceContainerLowest = NexTrueDarkSurfaceContainerLowest,
        surfaceContainerLow = NexTrueDarkSurfaceContainerLow,
        surfaceContainer = NexTrueDarkSurfaceContainer,
        surfaceContainerHigh = NexTrueDarkSurfaceContainerHigh,
        surfaceContainerHighest = NexTrueDarkSurfaceContainerHighest,
        surfaceDim = NexTrueDarkBackground,
        surfaceBright = NexTrueDarkSurfaceContainerHighest
    ) else base

    // Overlay the accent palette on top of the adjusted base.
    val palette = accentPalette(darkTheme, accent)
    return surfaceAdjusted.copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        surfaceTint = palette.primary
    )
}

/**
 * Creates a Typography where every sp value is multiplied by [fontScale].
 * Used when fontScale != 1.0 to honour the user's preferred text size.
 */
fun buildTypography(fontScale: Float): Typography = Typography(
    displaySmall = scaledTextStyle(fontScale, FontWeight.SemiBold, 36, 44, 0f),
    headlineLarge = scaledTextStyle(fontScale, FontWeight.SemiBold, 32, 40, 0f),
    headlineMedium = scaledTextStyle(fontScale, FontWeight.SemiBold, 28, 36, 0f),
    headlineSmall = scaledTextStyle(fontScale, FontWeight.SemiBold, 24, 32, 0f),
    bodyLarge = scaledTextStyle(fontScale, FontWeight.Normal, 16, 26, 0.15f),
    bodyMedium = scaledTextStyle(fontScale, FontWeight.Normal, 14, 20, 0.25f),
    bodySmall = scaledTextStyle(fontScale, FontWeight.Normal, 12, 16, 0.4f),
    titleLarge = scaledTextStyle(fontScale, FontWeight.SemiBold, 22, 28, 0f),
    titleMedium = scaledTextStyle(fontScale, FontWeight.Medium, 16, 24, 0.15f),
    titleSmall = scaledTextStyle(fontScale, FontWeight.Medium, 14, 20, 0.1f),
    labelLarge = scaledTextStyle(fontScale, FontWeight.SemiBold, 14, 20, 0.1f),
    labelSmall = scaledTextStyle(fontScale, FontWeight.Medium, 11, 16, 0.5f),
    labelMedium = scaledTextStyle(fontScale, FontWeight.Medium, 12, 16, 0.5f)
)

private fun scaledTextStyle(
    fontScale: Float,
    fontWeight: FontWeight,
    fontSizeSp: Int,
    lineHeightSp: Int,
    letterSpacingSp: Float
): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = fontWeight,
    fontSize = (fontSizeSp * fontScale).sp,
    lineHeight = (lineHeightSp * fontScale).sp,
    letterSpacing = letterSpacingSp.sp
)

/**
 * Main NexNote theme.
 *
 * [darkTheme]   - controls the base color scheme; follows system by default.
 * [trueDark]    - when true and dark, replaces surfaces with pure black (OLED).
 * [fontScale]   - sp multiplier applied to the entire typography (0.85 / 1.0 / 1.15).
 * [accentColor] - replaces the primary color family across the whole scheme.
 *
 * The selected accent acts as the app key color so existing color preferences
 * keep working across light, dark, system, and true-dark modes.
 */
@Composable
fun NexNoteTheme(
    darkTheme:   Boolean     = isSystemInDarkTheme(),
    trueDark:    Boolean     = false,
    fontScale:   Float       = 1.0f,
    accentColor: AccentColor = AccentColor.VIOLET,
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(darkTheme, trueDark, accentColor)
    val typography  = if (fontScale == 1.0f) NexNoteTypography else buildTypography(fontScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = NexNoteShapes,
        content     = content
    )
}
