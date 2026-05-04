package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ── Default palette: Violet / Indigo (Material 3) ─────────────────────────────

val NexPrimary               = Color(0xFF6650A4)
val NexOnPrimary             = Color(0xFFFFFFFF)
val NexPrimaryContainer      = Color(0xFFEADDFF)
val NexOnPrimaryContainer    = Color(0xFF21005D)

val NexPrimaryDark           = Color(0xFFD0BCFF)
val NexOnPrimaryDark         = Color(0xFF381E72)
val NexPrimaryContainerDark  = Color(0xFF4F378B)
val NexOnPrimaryContainerDark = Color(0xFFEADDFF)

// ── Blue accent ────────────────────────────────────────────────────────────────

val NexBluePrimary               = Color(0xFF1565C0)
val NexBlueOnPrimary             = Color(0xFFFFFFFF)
val NexBluePrimaryContainer      = Color(0xFFD3E4FF)
val NexBlueOnPrimaryContainer    = Color(0xFF001B3E)

val NexBluePrimaryDark           = Color(0xFFA8C8FF)
val NexBlueOnPrimaryDark         = Color(0xFF003063)
val NexBluePrimaryContainerDark  = Color(0xFF00419E)
val NexBlueOnPrimaryContainerDark = Color(0xFFD3E4FF)

// ── Green accent ───────────────────────────────────────────────────────────────

val NexGreenPrimary               = Color(0xFF2D6A2F)
val NexGreenOnPrimary             = Color(0xFFFFFFFF)
val NexGreenPrimaryContainer      = Color(0xFFAFFDBF)
val NexGreenOnPrimaryContainer    = Color(0xFF002106)

val NexGreenPrimaryDark           = Color(0xFF94E69B)
val NexGreenOnPrimaryDark         = Color(0xFF003913)
val NexGreenPrimaryContainerDark  = Color(0xFF1C521E)
val NexGreenOnPrimaryContainerDark = Color(0xFFAFFDBF)

// ── Orange accent ──────────────────────────────────────────────────────────────

val NexOrangePrimary               = Color(0xFF8B4000)
val NexOrangeOnPrimary             = Color(0xFFFFFFFF)
val NexOrangePrimaryContainer      = Color(0xFFFFDCC3)
val NexOrangeOnPrimaryContainer    = Color(0xFF2F1100)

val NexOrangePrimaryDark           = Color(0xFFFFB686)
val NexOrangeOnPrimaryDark         = Color(0xFF4C2000)
val NexOrangePrimaryContainerDark  = Color(0xFF6E2F00)
val NexOrangeOnPrimaryContainerDark = Color(0xFFFFDCC3)

// ── Red accent ─────────────────────────────────────────────────────────────────

val NexRedPrimary               = Color(0xFFBA1A1A)
val NexRedOnPrimary             = Color(0xFFFFFFFF)
val NexRedPrimaryContainer      = Color(0xFFFFDAD6)
val NexRedOnPrimaryContainer    = Color(0xFF410002)

val NexRedPrimaryDark           = Color(0xFFFFB4AB)
val NexRedOnPrimaryDark         = Color(0xFF690005)
val NexRedPrimaryContainerDark  = Color(0xFF93000A)
val NexRedOnPrimaryContainerDark = Color(0xFFFFDAD6)

// ── Teal accent ────────────────────────────────────────────────────────────────

val NexTealPrimary               = Color(0xFF006A60)
val NexTealOnPrimary             = Color(0xFFFFFFFF)
val NexTealPrimaryContainer      = Color(0xFF72F8EB)
val NexTealOnPrimaryContainer    = Color(0xFF00201C)

val NexTealPrimaryDark           = Color(0xFF50DBCD)
val NexTealOnPrimaryDark         = Color(0xFF00201C)
val NexTealPrimaryContainerDark  = Color(0xFF004F47)
val NexTealOnPrimaryContainerDark = Color(0xFF72F8EB)

// ── Surface / Background ──────────────────────────────────────────────────────

val NexBackground        = Color(0xFFFFFBFE)
val NexSurface           = Color(0xFFFFFBFE)
val NexOnSurface         = Color(0xFF1C1B1F)
val NexSurfaceVariant    = Color(0xFFE7E0EC)

// Dark mode: slightly lightened purple-tinted dark surfaces for better readability.
val NexBackgroundDark              = Color(0xFF211F2D)
val NexSurfaceDark                 = Color(0xFF211F2D)
val NexOnSurfaceDark               = Color(0xFFE6E1E5)
val NexOnSurfaceVariantDark        = Color(0xFFCAC4D0)
val NexSurfaceVariantDark          = Color(0xFF49454F)
val NexSurfaceContainerLowestDark  = Color(0xFF151321)
val NexSurfaceContainerLowDark     = Color(0xFF282636)
val NexSurfaceContainerDark        = Color(0xFF2E2B3D)
val NexSurfaceContainerHighDark    = Color(0xFF343243)
val NexSurfaceContainerHighestDark = Color(0xFF3F3D4F)

// True dark: pure black surfaces for OLED displays.
val NexTrueDarkBackground              = Color(0xFF000000)
val NexTrueDarkSurface                 = Color(0xFF000000)
val NexTrueDarkSurfaceVariant          = Color(0xFF1A1A1A)
val NexTrueDarkSurfaceContainerLowest  = Color(0xFF000000)
val NexTrueDarkSurfaceContainerLow     = Color(0xFF0F0D1C)
val NexTrueDarkSurfaceContainer        = Color(0xFF141220)
val NexTrueDarkSurfaceContainerHigh    = Color(0xFF181624)
val NexTrueDarkSurfaceContainerHighest = Color(0xFF201E2E)

// ── Secondary ─────────────────────────────────────────────────────────────────

val NexSecondary     = Color(0xFF625B71)
val NexSecondaryDark = Color(0xFFCCC2DC)

// ── Error ─────────────────────────────────────────────────────────────────────

val NexError     = Color(0xFFB3261E)
val NexErrorDark = Color(0xFFF2B8B5)

// ── Outline / Divider ─────────────────────────────────────────────────────────

val NexOutline     = Color(0xFF79747E)
val NexOutlineDark = Color(0xFF938F99)

// ── Note background color adaptation ──────────────────────────────────────────

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
