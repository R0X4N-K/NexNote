package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.MarkdownSourceRange

private const val READING_PROGRESS_ANIMATION_MS = 120
private const val READING_PROGRESS_ANCHOR_FRACTION = 0.35f

private val ScrollTrackWidth = 3.dp
private val ScrollTrackTouchWidth = 12.dp
private val ScrollTrackVerticalPadding = 8.dp
private val ScrollTrackCornerRadius = 2.dp
private val ScrollThumbMinHeight = 24.dp

/**
 * Vertical scroll-position indicator displayed on the right edge of the
 * preview. Draws a thin track with a rounded thumb whose vertical offset
 * reflects how far the user has scrolled through the markdown content.
 *
 * The progress value drives the thumb position directly (no tweened
 * animation) so the thumb follows the user's finger in real time instead
 * of lagging behind and jumping when the scroll settles.
 */
@Composable
internal fun EditorPreviewReadingProgressBar(
    lazyListState: LazyListState,
    sourceRanges: List<MarkdownSourceRange>,
    contentLength: Int,
    modifier: Modifier = Modifier
) {
    val state by rememberPreviewReadingProgress(lazyListState, sourceRanges, contentLength)

    EditorScrollPositionBar(
        state = state,
        progressDescription = "Reading progress",
        modifier = modifier
    )
}

/**
 * Vertical scroll-position indicator for edit mode.
 *
 * Edit mode is backed by a state-based [androidx.compose.foundation.text.BasicTextField],
 * so the thumb is driven by the field's pixel [ScrollState] instead of markdown
 * source ranges.
 */
@Composable
internal fun EditorEditReadingProgressBar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val state by rememberEditScrollProgress(scrollState)

    EditorScrollPositionBar(
        state = state,
        progressDescription = "Editor scroll progress",
        modifier = modifier
    )
}

@Composable
private fun EditorScrollPositionBar(
    state: ScrollProgressState,
    progressDescription: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS)),
        exit = fadeOut(animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS)),
        modifier = modifier
    ) {
        VerticalScrollPositionTrack(
            progress = state.progress,
            trackColor = trackColor,
            thumbColor = thumbColor,
            modifier = Modifier
                .fillMaxHeight()
                .width(ScrollTrackTouchWidth)
                .padding(vertical = ScrollTrackVerticalPadding)
                .semantics { contentDescription = progressDescription }
        )
    }
}

/**
 * Custom-drawn vertical track with a rounded thumb. [progress] (0f..1f)
 * controls the thumb's vertical position along the track.
 */
@Composable
private fun VerticalScrollPositionTrack(
    progress: Float,
    trackColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val trackWidthPx = ScrollTrackWidth.toPx()
        val cornerRadiusPx = ScrollTrackCornerRadius.toPx()
        val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        val trackLeft = (size.width - trackWidthPx) / 2f

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(trackLeft, 0f),
            size = Size(trackWidthPx, size.height),
            cornerRadius = cornerRadius
        )

        val thumbHeightPx = ScrollThumbMinHeight.toPx().coerceAtMost(size.height)
        val maxThumbOffset = (size.height - thumbHeightPx).coerceAtLeast(0f)
        val thumbTop = maxThumbOffset * progress.coerceIn(0f, 1f)

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(trackLeft, thumbTop),
            size = Size(trackWidthPx, thumbHeightPx),
            cornerRadius = cornerRadius
        )
    }
}

@Composable
private fun rememberPreviewReadingProgress(
    lazyListState: LazyListState,
    sourceRanges: List<MarkdownSourceRange>,
    contentLength: Int
): State<ScrollProgressState> {
    return remember(lazyListState, sourceRanges, contentLength) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val sourceOffset = if (sourceRanges.isNotEmpty()) {
                sourceOffsetForPreviewViewportAnchor(
                    sourceRanges = sourceRanges,
                    layoutInfo = layoutInfo,
                    viewportFraction = READING_PROGRESS_ANCHOR_FRACTION
                )
            } else {
                0
            }
            val canScrollBackward = lazyListState.canScrollBackward
            val canScrollForward = lazyListState.canScrollForward
            ScrollProgressState(
                progress = previewReadingProgress(
                    contentLength = contentLength,
                    sourceOffset = sourceOffset,
                    canScrollBackward = canScrollBackward,
                    canScrollForward = canScrollForward
                ),
                isVisible = previewReadingProgressVisible(
                    canScrollBackward = canScrollBackward,
                    canScrollForward = canScrollForward
                )
            )
        }
    }
}

@Composable
private fun rememberEditScrollProgress(
    scrollState: ScrollState
): State<ScrollProgressState> {
    return remember(scrollState) {
        derivedStateOf {
            ScrollProgressState(
                progress = editScrollProgress(
                    scrollOffset = scrollState.value,
                    maxScrollOffset = scrollState.maxValue
                ),
                isVisible = editScrollProgressVisible(
                    maxScrollOffset = scrollState.maxValue
                )
            )
        }
    }
}

private data class ScrollProgressState(
    val progress: Float,
    val isVisible: Boolean
)

internal fun previewReadingProgress(
    contentLength: Int,
    sourceOffset: Int,
    canScrollBackward: Boolean,
    canScrollForward: Boolean
): Float {
    if (contentLength <= 0) return 1f
    if (!canScrollBackward && !canScrollForward) return 1f
    if (!canScrollBackward) return 0f
    if (!canScrollForward) return 1f

    return (sourceOffset.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
}

internal fun previewReadingProgressVisible(
    canScrollBackward: Boolean,
    canScrollForward: Boolean
): Boolean = canScrollBackward || canScrollForward

internal fun editScrollProgress(
    scrollOffset: Int,
    maxScrollOffset: Int
): Float {
    if (maxScrollOffset <= 0) return 1f

    return (scrollOffset.toFloat() / maxScrollOffset.toFloat()).coerceIn(0f, 1f)
}

internal fun editScrollProgressVisible(maxScrollOffset: Int): Boolean =
    maxScrollOffset > 0
