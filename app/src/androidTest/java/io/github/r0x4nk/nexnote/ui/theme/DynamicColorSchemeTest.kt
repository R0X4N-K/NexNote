package io.github.r0x4nk.nexnote.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.NOTE_COLOR_PALETTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicColorSchemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nestedNoteThemesPreserveOriginalPaletteForColorPickers() {
        val dark = mutableStateOf(false)
        compose.setContent {
            NexNoteTheme(darkTheme = dark.value, accentColor = AccentColor.SAGE) {
                val base = MaterialTheme.colorScheme
                val editorColors = rememberNoteColors(NOTE_COLOR_PALETTE[3])
                NoteContentTheme(editorColors) {
                    for (stored in NOTE_COLOR_PALETTE) {
                        val expected = resolveNoteColors(stored, base, dark.value)
                        val swatch = rememberNoteColors(stored)
                        NoteContentTheme(swatch) {
                            val nested = rememberNoteColors(stored)
                            SideEffect {
                                assertEquals(expected, swatch)
                                assertEquals(expected, nested)
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
    }

    @Test
    fun optInUsesDevicePaletteAndRetainsReadableNotes() {
        val dark = mutableStateOf(false)
        val dynamic = mutableStateOf(true)
        compose.setContent {
            val context = LocalContext.current
            NexNoteTheme(darkTheme = dark.value, dynamicColor = dynamic.value, accentColor = AccentColor.SAGE) {
                val actual = MaterialTheme.colorScheme
                val expected = if (dynamic.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (dark.value) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else buildColorScheme(dark.value, false, AccentColor.SAGE)
                SideEffect {
                    assertEquals(expected.primary, actual.primary)
                    assertEquals(expected.secondaryContainer, actual.secondaryContainer)
                    for (stored in NOTE_COLOR_PALETTE) {
                        val note = resolveNoteColors(stored, actual, dark.value)
                        assertTrue(contrastRatio(note.onContainer, note.container) >= 7.0)
                        assertTrue(contrastRatio(note.accent, note.container) >= 4.5)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
        compose.runOnIdle { dynamic.value = false }
        compose.waitForIdle()
    }
}
