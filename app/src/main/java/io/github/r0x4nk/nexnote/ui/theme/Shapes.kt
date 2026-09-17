package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val NexNoteShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** Extended reference shapes kept separate from the stable Material 3 shape scale. */
internal object NexNoteExtendedShapes {
    val largeIncreased = RoundedCornerShape(22.dp)
    val extraLargeIncreased = RoundedCornerShape(28.dp)
    val extraExtraLarge = RoundedCornerShape(36.dp)
    val compactControl = RoundedCornerShape(12.dp)
}
