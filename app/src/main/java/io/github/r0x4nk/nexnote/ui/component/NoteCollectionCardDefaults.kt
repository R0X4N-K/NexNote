package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object NoteCollectionCardDefaults {
    val shape: Shape = io.github.r0x4nk.nexnote.ui.theme.NexNoteShapes.medium
    val borderWidth: Dp = 1.dp
    val defaultElevation: Dp = 0.dp

    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

    @Composable
    fun border(
        alpha: Float = 0.30f,
        color: Color = MaterialTheme.colorScheme.outlineVariant,
        width: Dp = borderWidth
    ): BorderStroke =
        BorderStroke(
            width = width,
            color = color.copy(alpha = alpha)
        )
}
