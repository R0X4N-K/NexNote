package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Draws the delete affordance revealed by a note card swipe.
 *
 * Material 3's swipe samples use a full-size background whose color follows
 * swipe progress. The ramp is ordered by luminance so the first touch is always
 * the lighter red and a wider swipe always moves toward the darker red, in both
 * light and dark themes.
 */
@Composable
internal fun NoteCardSwipeBackground(state: SwipeToDismissBoxState) {
    val visualState = swipeDeleteVisualState(state)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(visualState.backgroundColor())
            .padding(horizontal = SwipeDeleteTokens.HorizontalPadding),
        contentAlignment = visualState.anchor.iconAlignment
    ) {
        if (visualState.isVisible) {
            SwipeDeleteIcon(visualState)
        }
    }
}

@Composable
private fun swipeDeleteVisualState(state: SwipeToDismissBoxState): SwipeDeleteVisualState {
    val density = LocalDensity.current
    val swipeOffset = runCatching { state.requireOffset() }.getOrDefault(0f)
    val progress = with(density) {
        (abs(swipeOffset) / SwipeDeleteTokens.StretchDistance.toPx()).coerceIn(0f, 1f)
    }

    return SwipeDeleteVisualState(
        progress = progress,
        thresholdReached = state.targetValue == SwipeToDismissBoxValue.EndToStart ||
            state.currentValue == SwipeToDismissBoxValue.EndToStart,
        anchor = resolveSwipeDeleteAnchor(swipeOffset, state)
    )
}

private fun resolveSwipeDeleteAnchor(
    swipeOffset: Float,
    state: SwipeToDismissBoxState
): SwipeDeleteAnchor =
    when {
        swipeOffset > 0f -> SwipeDeleteAnchor.Start
        state.targetValue == SwipeToDismissBoxValue.StartToEnd -> SwipeDeleteAnchor.Start
        state.currentValue == SwipeToDismissBoxValue.StartToEnd -> SwipeDeleteAnchor.Start
        else -> SwipeDeleteAnchor.End
    }

@Composable
private fun SwipeDeleteIcon(visualState: SwipeDeleteVisualState) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Move to trash",
        tint = visualState.iconColor(),
        modifier = Modifier
            .size(SwipeDeleteTokens.IconSize)
            .graphicsLayer {
                alpha = visualState.iconAlpha
                val scale = visualState.iconScale
                scaleX = scale
                scaleY = scale
            }
    )
}

private data class SwipeDeleteVisualState(
    val progress: Float,
    val thresholdReached: Boolean,
    val anchor: SwipeDeleteAnchor
) {
    val isVisible: Boolean
        get() = progress > SwipeDeleteTokens.MinVisibleProgress || thresholdReached

    val emphasis: Float
        get() = if (thresholdReached) {
            progress.coerceAtLeast(SwipeDeleteTokens.ThresholdEmphasis)
        } else {
            progress
        }

    val iconAlpha: Float
        get() = SwipeDeleteTokens.MinIconAlpha +
            (SwipeDeleteTokens.MaxIconAlpha - SwipeDeleteTokens.MinIconAlpha) * emphasis

    val iconScale: Float
        get() = SwipeDeleteTokens.MinIconScale +
            (SwipeDeleteTokens.MaxIconScale - SwipeDeleteTokens.MinIconScale) * emphasis

    @Composable
    fun backgroundColor(): Color {
        if (!isVisible) return Color.Transparent
        val lowEmphasisColor = lighterOf(
            first = MaterialTheme.colorScheme.errorContainer,
            second = MaterialTheme.colorScheme.error
        )
        val highEmphasisColor = darkerOf(
            first = MaterialTheme.colorScheme.errorContainer,
            second = MaterialTheme.colorScheme.error
        )

        return lerp(
            start = lowEmphasisColor,
            stop = highEmphasisColor,
            fraction = emphasis
        )
    }

    @Composable
    fun iconColor(): Color =
        if (backgroundColor().luminance() > SwipeDeleteTokens.LightBackgroundThreshold) {
            Color.Black
        } else {
            Color.White
        }
}

private fun lighterOf(first: Color, second: Color): Color =
    if (first.luminance() >= second.luminance()) first else second

private fun darkerOf(first: Color, second: Color): Color =
    if (first.luminance() < second.luminance()) first else second

private enum class SwipeDeleteAnchor(
    val iconAlignment: Alignment
) {
    Start(iconAlignment = Alignment.CenterStart),
    End(iconAlignment = Alignment.CenterEnd)
}

private object SwipeDeleteTokens {
    val HorizontalPadding = 24.dp
    val StretchDistance = 160.dp
    val IconSize = 24.dp

    const val MinVisibleProgress = 0.04f
    const val ThresholdEmphasis = 0.72f
    const val MinIconAlpha = 0.68f
    const val MaxIconAlpha = 1f
    const val MinIconScale = 0.96f
    const val MaxIconScale = 1.12f
    const val LightBackgroundThreshold = 0.45f
}
