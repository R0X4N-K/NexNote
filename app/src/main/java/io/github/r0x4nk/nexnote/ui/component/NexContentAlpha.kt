package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.theme.readableColor

internal object NexContentAlpha {
    const val Body = 0.82f
    const val Metadata = 0.68f
}

/** Resolve opacity against the painted card, retaining readable contrast. */
@Composable
internal fun noteCardSecondaryColor(alpha: Float): Color {
    val scheme = MaterialTheme.colorScheme
    val background = scheme.surfaceColorAtElevation(1.dp)
    val foreground = scheme.onSurface
    return remember(foreground, background, alpha) {
        readableColor(foreground.copy(alpha = alpha).compositeOver(background), background)
    }
}
