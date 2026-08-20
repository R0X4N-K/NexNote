package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
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

/** Draws the directional action revealed behind a collection card. */
@Composable
internal fun SwipeCollectionActionBackground(
    state: SwipeToDismissBoxState,
    endToStartAction: SwipeCollectionAction,
    startToEndAction: SwipeCollectionAction?
) {
    val visualState = swipeCollectionActionVisualState(
        state = state,
        endToStartAction = endToStartAction,
        startToEndAction = startToEndAction
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(visualState.backgroundColor())
            .padding(horizontal = SwipeCollectionActionTokens.HorizontalPadding),
        contentAlignment = visualState.anchor.iconAlignment
    ) {
        if (visualState.isVisible) {
            SwipeCollectionActionIcon(visualState)
        }
    }
}

@Composable
private fun swipeCollectionActionVisualState(
    state: SwipeToDismissBoxState,
    endToStartAction: SwipeCollectionAction,
    startToEndAction: SwipeCollectionAction?
): SwipeCollectionActionVisualState {
    val density = LocalDensity.current
    val swipeOffset = runCatching { state.requireOffset() }.getOrDefault(0f)
    val anchor = resolveSwipeCollectionActionAnchor(swipeOffset, state)
    val action = if (anchor == SwipeCollectionActionAnchor.Start) {
        startToEndAction ?: endToStartAction
    } else {
        endToStartAction
    }
    val progress = with(density) {
        (abs(swipeOffset) / SwipeCollectionActionTokens.StretchDistance.toPx())
            .coerceIn(0f, 1f)
    }
    val thresholdReached = when (anchor) {
        SwipeCollectionActionAnchor.Start ->
            state.targetValue == SwipeToDismissBoxValue.StartToEnd ||
                state.currentValue == SwipeToDismissBoxValue.StartToEnd
        SwipeCollectionActionAnchor.End ->
            state.targetValue == SwipeToDismissBoxValue.EndToStart ||
                state.currentValue == SwipeToDismissBoxValue.EndToStart
    }

    return SwipeCollectionActionVisualState(
        progress = progress,
        thresholdReached = thresholdReached,
        anchor = anchor,
        action = action
    )
}

private fun resolveSwipeCollectionActionAnchor(
    swipeOffset: Float,
    state: SwipeToDismissBoxState
): SwipeCollectionActionAnchor =
    when {
        swipeOffset > 0f -> SwipeCollectionActionAnchor.Start
        state.targetValue == SwipeToDismissBoxValue.StartToEnd ->
            SwipeCollectionActionAnchor.Start
        state.currentValue == SwipeToDismissBoxValue.StartToEnd ->
            SwipeCollectionActionAnchor.Start
        else -> SwipeCollectionActionAnchor.End
    }

@Composable
private fun SwipeCollectionActionIcon(visualState: SwipeCollectionActionVisualState) {
    val icon = when (val action = visualState.action) {
        is SwipeCollectionAction.Delete -> Icons.Default.Delete
        is SwipeCollectionAction.TogglePin -> {
            if (action.isCurrentlyPinned) Icons.Default.PushPin else Icons.Outlined.PushPin
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = visualState.action.contentDescription,
        tint = visualState.iconColor(),
        modifier = Modifier
            .size(SwipeCollectionActionTokens.IconSize)
            .graphicsLayer {
                alpha = visualState.iconAlpha
                val scale = visualState.iconScale
                scaleX = scale
                scaleY = scale
            }
    )
}

private data class SwipeCollectionActionVisualState(
    val progress: Float,
    val thresholdReached: Boolean,
    val anchor: SwipeCollectionActionAnchor,
    val action: SwipeCollectionAction
) {
    val isVisible: Boolean
        get() = progress > SwipeCollectionActionTokens.MinVisibleProgress || thresholdReached

    val emphasis: Float
        get() = if (thresholdReached) {
            progress.coerceAtLeast(SwipeCollectionActionTokens.ThresholdEmphasis)
        } else {
            progress
        }

    val iconAlpha: Float
        get() = SwipeCollectionActionTokens.MinIconAlpha +
            (SwipeCollectionActionTokens.MaxIconAlpha -
                SwipeCollectionActionTokens.MinIconAlpha) * emphasis

    val iconScale: Float
        get() = SwipeCollectionActionTokens.MinIconScale +
            (SwipeCollectionActionTokens.MaxIconScale -
                SwipeCollectionActionTokens.MinIconScale) * emphasis

    @Composable
    fun backgroundColor(): Color {
        if (!isVisible) return Color.Transparent
        val target = when (action) {
            is SwipeCollectionAction.Delete -> MaterialTheme.colorScheme.errorContainer
            is SwipeCollectionAction.TogglePin -> MaterialTheme.colorScheme.primaryContainer
        }
        return lerp(
            start = MaterialTheme.colorScheme.surfaceContainerHighest,
            stop = target,
            fraction = 0.34f + 0.34f * emphasis
        )
    }

    @Composable
    fun iconColor(): Color {
        val background = backgroundColor()
        return when (action) {
            is SwipeCollectionAction.Delete -> {
                if (background.luminance() > SwipeCollectionActionTokens.LightBackgroundThreshold) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            }
            is SwipeCollectionAction.TogglePin -> {
                if (background.luminance() > SwipeCollectionActionTokens.LightBackgroundThreshold) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            }
        }
    }
}

private enum class SwipeCollectionActionAnchor(
    val iconAlignment: Alignment
) {
    Start(iconAlignment = Alignment.CenterStart),
    End(iconAlignment = Alignment.CenterEnd)
}

private object SwipeCollectionActionTokens {
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
