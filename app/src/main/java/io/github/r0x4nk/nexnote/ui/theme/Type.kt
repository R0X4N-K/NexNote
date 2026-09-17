package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Builds the app typography.
 *
 * Every sp value is multiplied by [fontScale] and every style uses [fontFamily],
 * so both the user's text-size and typeface preferences apply consistently to
 * the whole interface.
 */
fun buildNexNoteTypography(
    fontScale: Float,
    fontFamily: FontFamily
): Typography = Typography(
    displaySmall = scaledTextStyle(fontFamily, fontScale, FontWeight.SemiBold, 36, 44, 0f),
    headlineLarge = scaledTextStyle(fontFamily, fontScale, FontWeight.SemiBold, 32, 40, 0f),
    headlineMedium = scaledTextStyle(fontFamily, fontScale, FontWeight.SemiBold, 28, 36, 0f),
    headlineSmall = scaledTextStyle(fontFamily, fontScale, FontWeight.SemiBold, 24, 32, 0f),
    bodyLarge = scaledTextStyle(fontFamily, fontScale, FontWeight.Normal, 16, 26, 0.15f),
    bodyMedium = scaledTextStyle(fontFamily, fontScale, FontWeight.Normal, 14, 20, 0.25f),
    bodySmall = scaledTextStyle(fontFamily, fontScale, FontWeight.Normal, 12, 16, 0.4f),
    titleLarge = scaledTextStyle(fontFamily, fontScale, FontWeight.SemiBold, 22, 28, 0f),
    titleMedium = scaledTextStyle(fontFamily, fontScale, FontWeight.Medium, 16, 24, 0.15f),
    titleSmall = scaledTextStyle(fontFamily, fontScale, FontWeight.Medium, 14, 20, 0.1f),
    labelLarge = scaledTextStyle(fontFamily, fontScale, FontWeight.SemiBold, 14, 20, 0.1f),
    labelSmall = scaledTextStyle(fontFamily, fontScale, FontWeight.Medium, 11, 16, 0.5f),
    labelMedium = scaledTextStyle(fontFamily, fontScale, FontWeight.Medium, 12, 16, 0.5f)
)

/** Default typography kept for previews and tests that do not customise the theme. */
val NexNoteTypography: Typography = buildNexNoteTypography(1.0f, FontFamily.Default)

private fun scaledTextStyle(
    fontFamily: FontFamily,
    fontScale: Float,
    fontWeight: FontWeight,
    fontSizeSp: Int,
    lineHeightSp: Int,
    letterSpacingSp: Float
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = (fontSizeSp * fontScale).sp,
    lineHeight = (lineHeightSp * fontScale).sp,
    letterSpacing = letterSpacingSp.sp
)
