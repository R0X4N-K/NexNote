package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Paint only: does not change measurement, insets, focus or gesture handling. */
@Composable
internal fun Modifier.nexNoteBackground(): Modifier {
    val scheme = MaterialTheme.colorScheme
    val oled = LocalNexNoteOledTheme.current
    val base = if (oled) Color.Black else scheme.background
    val tint = scheme.primaryContainer.copy(alpha = 0.48f)
    return drawWithCache {
        val gradient = if (oled) null else Brush.verticalGradient(
            0f to tint,
            0.55f to tint.copy(alpha = 0f),
            1f to tint.copy(alpha = 0f),
            endY = size.height.coerceAtLeast(1f)
        )
        onDrawBehind {
            drawRect(base)
            gradient?.let { drawRect(it) }
        }
    }
}
