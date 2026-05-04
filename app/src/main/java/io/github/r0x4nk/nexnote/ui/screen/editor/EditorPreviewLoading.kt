package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorPreviewLoadingPlaceholder(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val placeholderColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.48f)
    val accentColor = colorScheme.primary.copy(alpha = 0.26f)

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { contentDescription = "Loading preview" }
    ) {
        PreviewLoadingHeader(placeholderColor = placeholderColor, accentColor = accentColor)
        repeat(4) { index ->
            PreviewLoadingParagraph(
                placeholderColor = placeholderColor,
                firstLineFraction = if (index % 2 == 0) 0.92f else 0.78f,
                lastLineFraction = if (index % 2 == 0) 0.64f else 0.48f
            )
        }
    }
}

@Composable
private fun PreviewLoadingHeader(
    placeholderColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PreviewLoadingBlock(
                color = accentColor,
                widthFraction = 0.18f,
                height = 28.dp,
                shape = CircleShape
            )
            PreviewLoadingBlock(
                color = placeholderColor,
                widthFraction = 0.36f,
                height = 28.dp
            )
        }
        PreviewLoadingBlock(
            color = placeholderColor,
            widthFraction = 0.82f,
            height = 34.dp
        )
    }
}

@Composable
private fun PreviewLoadingParagraph(
    placeholderColor: Color,
    firstLineFraction: Float,
    lastLineFraction: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewLoadingBlock(
            color = placeholderColor,
            widthFraction = firstLineFraction,
            height = 14.dp
        )
        PreviewLoadingBlock(
            color = placeholderColor,
            widthFraction = 0.98f,
            height = 14.dp
        )
        PreviewLoadingBlock(
            color = placeholderColor,
            widthFraction = lastLineFraction,
            height = 14.dp
        )
    }
}

@Composable
private fun PreviewLoadingBlock(
    color: Color,
    widthFraction: Float,
    height: Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction.coerceIn(0f, 1f))
            .height(height)
            .clip(shape)
            .background(color)
    )
}
