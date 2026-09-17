package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.NOTE_COLOR_PALETTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun `all note colors retain readable content for every accent and display mode`() {
        for (accent in AccentColor.entries) for (dark in listOf(false, true)) for (oled in listOf(false, true)) {
            val scheme = buildColorScheme(dark, oled, accent)
            // Imported or previously stored arbitrary colors use the same resolver as preset colors.
            for (stored in NOTE_COLOR_PALETTE + listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF00FF00.toInt())) {
                val colors = resolveNoteColors(stored, scheme, dark)
                val context = "$accent dark=$dark oled=$oled stored=$stored"
                assertContrast(context, colors.onContainer, colors.container, 7.0)
                assertContrast(context, colors.onContainerVariant, colors.container, 4.5)
                assertContrast(context, colors.accent, colors.container, 4.5)
                assertContrast(context, colors.outline, colors.container, 3.0)
                assertContrast(context, colors.onCodeContainer, colors.codeContainer, 4.5)
                assertEquals(1f, colors.container.alpha)
            }
        }
    }

    @Test
    fun `manual schemes have accessible paired roles and ordered surface containers`() {
        for (accent in AccentColor.entries) for (dark in listOf(false, true)) {
            val scheme = buildColorScheme(dark, false, accent)
            with(scheme) {
                listOf(primary to onPrimary, secondary to onSecondary, tertiary to onTertiary,
                    primaryContainer to onPrimaryContainer, secondaryContainer to onSecondaryContainer,
                    tertiaryContainer to onTertiaryContainer, error to onError, errorContainer to onErrorContainer,
                    surface to onSurface, surfaceContainerHighest to onSurfaceVariant,
                    inverseSurface to inverseOnSurface).forEach { (background, foreground) ->
                    assertContrast("$accent dark=$dark", foreground, background, 4.5)
                }
                val surfaces = listOf(surfaceContainerLowest, surfaceContainerLow, surfaceContainer,
                    surfaceContainerHigh, surfaceContainerHighest)
                surfaces.zipWithNext().forEach { (a, b) ->
                    val toneA = com.materialkolor.hct.Hct.fromInt(a.toArgb()).tone
                    val toneB = com.materialkolor.hct.Hct.fromInt(b.toArgb()).tone
                    assertTrue(if (dark) toneA < toneB else toneA > toneB)
                }
            }
        }
    }

    @Test
    fun `fixed roles retain their accent and contrast across appearance modes`() {
        for (accent in AccentColor.entries) {
            val light = buildColorScheme(false, false, accent)
            val dark = buildColorScheme(true, false, accent)
            assertEquals(light.primaryFixed, dark.primaryFixed)
            assertEquals(light.secondaryFixed, dark.secondaryFixed)
            assertEquals(light.tertiaryFixed, dark.tertiaryFixed)
            with(light) {
                listOf(
                    primaryFixed to onPrimaryFixed, primaryFixedDim to onPrimaryFixedVariant,
                    secondaryFixed to onSecondaryFixed, secondaryFixedDim to onSecondaryFixedVariant,
                    tertiaryFixed to onTertiaryFixed, tertiaryFixedDim to onTertiaryFixedVariant
                ).forEach { (background, foreground) ->
                    assertContrast("$accent fixed", foreground, background, 4.5)
                }
            }
        }
        assertNotEquals(buildColorScheme(false, false, AccentColor.VIOLET).primaryFixed,
            buildColorScheme(false, false, AccentColor.GREEN).primaryFixed)
    }

    @Test
    fun `changing accent updates secondary and inverse roles too`() {
        val violet = buildColorScheme(false, false, AccentColor.VIOLET)
        val green = buildColorScheme(false, false, AccentColor.GREEN)
        assertNotEquals(violet.secondary, green.secondary)
        assertNotEquals(violet.inversePrimary, green.inversePrimary)
        assertNotEquals(violet.surface, green.surface)
    }

    @Test
    fun `OLED override preserves colored containers and only applies in dark mode`() {
        val dark = buildColorScheme(true, false, AccentColor.SAGE)
        val oled = buildColorScheme(true, true, AccentColor.SAGE)
        assertEquals(Color.Black, oled.surface)
        assertEquals(dark.surfaceContainerLow, oled.surfaceContainerLow)
        assertEquals(dark.primary, oled.primary)
        assertEquals(buildColorScheme(false, false, AccentColor.SAGE).surface, buildColorScheme(false, true, AccentColor.SAGE).surface)
    }

    private fun assertContrast(context: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$context: $foreground on $background = $ratio, expected $minimum", ratio >= minimum)
    }
}
