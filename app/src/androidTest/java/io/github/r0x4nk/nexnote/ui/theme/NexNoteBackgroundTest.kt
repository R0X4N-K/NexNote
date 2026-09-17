package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class NexNoteBackgroundTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun themeChangesUpdateGradientAndOledRemovesItCompletely() {
        val dark = mutableStateOf(false)
        val oled = mutableStateOf(false)
        var background = Color.Unspecified
        compose.setContent {
            NexNoteTheme(darkTheme = dark.value, trueDark = oled.value) {
                background = MaterialTheme.colorScheme.background
                Box(Modifier.fillMaxSize().nexNoteBackground())
            }
        }
        fun assertGradient() {
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            assertNotEquals(pixels[0, 0], pixels[0, pixels.height - 1])
            assertEquals(background, pixels[0, pixels.height - 1])
        }
        assertGradient()
        compose.runOnIdle { dark.value = true }
        assertGradient()
        compose.runOnIdle { oled.value = true }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        for (y in 0 until pixels.height) {
            assertEquals(Color.Black, pixels[pixels.width / 2, y])
        }
        // True-dark remains dormant in light mode, just like the saved preference.
        compose.runOnIdle { dark.value = false }
        assertGradient()
    }
}
