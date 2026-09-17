package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Decorative, theme-aware artwork; the adjacent text carries its accessible meaning. */
@Composable
internal fun EmptyNotesIllustration(icon: ImageVector) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(168.dp, 144.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val unit = size.width / 168f
            drawOval(colors.primaryContainer.copy(alpha = 0.45f))
            rotate(-12f) {
                drawRoundRect(
                    color = colors.secondaryContainer,
                    topLeft = Offset(36 * unit, 18 * unit),
                    size = Size(92 * unit, 110 * unit),
                    cornerRadius = CornerRadius(14 * unit)
                )
            }
            drawRoundRect(
                color = colors.surfaceContainerHigh,
                topLeft = Offset(44 * unit, 12 * unit),
                size = Size(92 * unit, 110 * unit),
                cornerRadius = CornerRadius(14 * unit)
            )
            listOf(52f, 38f, 46f).forEachIndexed { index, width ->
                drawRoundRect(
                    color = colors.onSurfaceVariant.copy(alpha = 0.4f),
                    topLeft = Offset(62 * unit, (40 + index * 17) * unit),
                    size = Size(width * unit, 5 * unit),
                    cornerRadius = CornerRadius(3 * unit)
                )
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp),
            shape = MaterialTheme.shapes.large,
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp
        ) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}
