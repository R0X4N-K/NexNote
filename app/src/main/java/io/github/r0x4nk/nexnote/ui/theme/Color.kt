package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Default palette: expressive violet / indigo.

val NexPrimary                = Color(0xFF5654D4)
val NexOnPrimary              = Color(0xFFFFFFFF)
val NexPrimaryContainer       = Color(0xFFE2E0FF)
val NexOnPrimaryContainer     = Color(0xFF15134A)

val NexPrimaryDark            = Color(0xFFC4C1FF)
val NexOnPrimaryDark          = Color(0xFF26236E)
val NexPrimaryContainerDark   = Color(0xFF3F3B9C)
val NexOnPrimaryContainerDark = Color(0xFFE2E0FF)

// Blue accent.

val NexBluePrimary                = Color(0xFF0061A6)
val NexBlueOnPrimary              = Color(0xFFFFFFFF)
val NexBluePrimaryContainer       = Color(0xFFD2E4FF)
val NexBlueOnPrimaryContainer     = Color(0xFF001C38)

val NexBluePrimaryDark            = Color(0xFFA0CAFF)
val NexBlueOnPrimaryDark          = Color(0xFF00325A)
val NexBluePrimaryContainerDark   = Color(0xFF00497F)
val NexBlueOnPrimaryContainerDark = Color(0xFFD2E4FF)

// Green accent.

val NexGreenPrimary                = Color(0xFF3D6B1F)
val NexGreenOnPrimary              = Color(0xFFFFFFFF)
val NexGreenPrimaryContainer       = Color(0xFFBEF397)
val NexGreenOnPrimaryContainer     = Color(0xFF0B2000)

val NexGreenPrimaryDark            = Color(0xFFA3D67E)
val NexGreenOnPrimaryDark          = Color(0xFF143800)
val NexGreenPrimaryContainerDark   = Color(0xFF2A5108)
val NexGreenOnPrimaryContainerDark = Color(0xFFBEF397)

// Orange accent.

val NexOrangePrimary                = Color(0xFF9B4400)
val NexOrangeOnPrimary              = Color(0xFFFFFFFF)
val NexOrangePrimaryContainer       = Color(0xFFFFDBC8)
val NexOrangeOnPrimaryContainer     = Color(0xFF331200)

val NexOrangePrimaryDark            = Color(0xFFFFB68F)
val NexOrangeOnPrimaryDark          = Color(0xFF552100)
val NexOrangePrimaryContainerDark   = Color(0xFF763300)
val NexOrangeOnPrimaryContainerDark = Color(0xFFFFDBC8)

// Red accent.

val NexRedPrimary               = Color(0xFFBA1A1A)
val NexRedOnPrimary             = Color(0xFFFFFFFF)
val NexRedPrimaryContainer      = Color(0xFFFFDAD6)
val NexRedOnPrimaryContainer    = Color(0xFF410002)

val NexRedPrimaryDark           = Color(0xFFFFB4AB)
val NexRedOnPrimaryDark         = Color(0xFF690005)
val NexRedPrimaryContainerDark  = Color(0xFF93000A)
val NexRedOnPrimaryContainerDark = Color(0xFFFFDAD6)

// Teal accent.

val NexTealPrimary                = Color(0xFF006A6A)
val NexTealOnPrimary              = Color(0xFFFFFFFF)
val NexTealPrimaryContainer       = Color(0xFF70F7F6)
val NexTealOnPrimaryContainer     = Color(0xFF002020)

val NexTealPrimaryDark            = Color(0xFF4FDAD8)
val NexTealOnPrimaryDark          = Color(0xFF003737)
val NexTealPrimaryContainerDark   = Color(0xFF005050)
val NexTealOnPrimaryContainerDark = Color(0xFF70F7F6)

// Surface / background.

val NexBackground              = Color(0xFFFFFBFF)
val NexSurface                 = Color(0xFFFFFBFF)
val NexOnSurface               = Color(0xFF1B1B21)
val NexOnSurfaceVariant        = Color(0xFF47464F)
val NexSurfaceVariant          = Color(0xFFE7E1EC)
val NexSurfaceContainerLowest  = Color(0xFFFFFFFF)
val NexSurfaceContainerLow     = Color(0xFFF8F2FA)
val NexSurfaceContainer        = Color(0xFFF2ECF4)
val NexSurfaceContainerHigh    = Color(0xFFECE6EF)
val NexSurfaceContainerHighest = Color(0xFFE6E1E9)
val NexSurfaceDim              = Color(0xFFDDD7E0)
val NexSurfaceBright           = Color(0xFFFFFBFF)

// Dark mode: neutral-chroma surfaces keep dynamic accent colors readable.
val NexBackgroundDark              = Color(0xFF131318)
val NexSurfaceDark                 = Color(0xFF131318)
val NexOnSurfaceDark               = Color(0xFFE6E1E9)
val NexOnSurfaceVariantDark        = Color(0xFFC9C5D0)
val NexSurfaceVariantDark          = Color(0xFF484650)
val NexSurfaceContainerLowestDark  = Color(0xFF0D0D12)
val NexSurfaceContainerLowDark     = Color(0xFF1B1B21)
val NexSurfaceContainerDark        = Color(0xFF1F1F25)
val NexSurfaceContainerHighDark    = Color(0xFF2A2930)
val NexSurfaceContainerHighestDark = Color(0xFF35343B)
val NexSurfaceDimDark              = Color(0xFF131318)
val NexSurfaceBrightDark           = Color(0xFF393840)

// True dark: pure black surfaces for OLED displays.
val NexTrueDarkBackground              = Color(0xFF000000)
val NexTrueDarkSurface                 = Color(0xFF000000)
val NexTrueDarkSurfaceVariant          = Color(0xFF1D1B24)
val NexTrueDarkSurfaceContainerLowest  = Color(0xFF000000)
val NexTrueDarkSurfaceContainerLow     = Color(0xFF09090E)
val NexTrueDarkSurfaceContainer        = Color(0xFF0E0E14)
val NexTrueDarkSurfaceContainerHigh    = Color(0xFF17161D)
val NexTrueDarkSurfaceContainerHighest = Color(0xFF22212A)

// Secondary.

val NexSecondary                = Color(0xFF5D5D72)
val NexOnSecondary              = Color(0xFFFFFFFF)
val NexSecondaryContainer       = Color(0xFFE2E0F9)
val NexOnSecondaryContainer     = Color(0xFF1A1A2C)

val NexSecondaryDark            = Color(0xFFC6C4DD)
val NexOnSecondaryDark          = Color(0xFF2F2F43)
val NexSecondaryContainerDark   = Color(0xFF46455A)
val NexOnSecondaryContainerDark = Color(0xFFE2E0F9)

// Tertiary.

val NexTertiary                = Color(0xFF006A6A)
val NexOnTertiary              = Color(0xFFFFFFFF)
val NexTertiaryContainer       = Color(0xFF70F7F6)
val NexOnTertiaryContainer     = Color(0xFF002020)

val NexTertiaryDark            = Color(0xFF4FDAD8)
val NexOnTertiaryDark          = Color(0xFF003737)
val NexTertiaryContainerDark   = Color(0xFF005050)
val NexOnTertiaryContainerDark = Color(0xFF70F7F6)

// Error.

val NexError                = Color(0xFFBA1A1A)
val NexOnError              = Color(0xFFFFFFFF)
val NexErrorContainer       = Color(0xFFFFDAD6)
val NexOnErrorContainer     = Color(0xFF410002)

val NexErrorDark            = Color(0xFFFFB4AB)
val NexOnErrorDark          = Color(0xFF690005)
val NexErrorContainerDark   = Color(0xFF93000A)
val NexOnErrorContainerDark = Color(0xFFFFDAD6)

// Outline / divider.

val NexOutline        = Color(0xFF787680)
val NexOutlineVariant = Color(0xFFC9C5D0)

val NexOutlineDark        = Color(0xFF928F99)
val NexOutlineVariantDark = Color(0xFF484650)

// Inverse roles.

val NexInverseSurface        = Color(0xFF302F36)
val NexInverseOnSurface      = Color(0xFFF3EFF7)
val NexInversePrimary        = Color(0xFF5C58D6)
val NexInverseSurfaceDark    = Color(0xFFE6E1E9)
val NexInverseOnSurfaceDark  = Color(0xFF302F36)
val NexInversePrimaryDark    = Color(0xFF5654D4)

// Note background color adaptation.

/**
 * Returns the effective background color for a note, adapted to the current theme.
 * In dark mode the light pastel is blended 60 % toward black so that white text
 * stays readable; in light mode the stored color is used as-is.
 * Pass `surfaceIsDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f`.
 */
fun adaptNoteColor(storedColor: Int, surfaceIsDark: Boolean): Color {
    val base = Color(storedColor)
    return if (surfaceIsDark) lerp(base, Color.Black, 0.6f) else base
}
