package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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

/**
 * Vertical scroll position indicator displayed on the right edge of the
 * preview. Draws a thin track with a rounded thumb whose vertical offset
 * reflects how far the user has scrolled through the markdown content.
 *
 * Preferred over a horizontal progress bar because it mirrors the natural
 * scroll direction and stays out of the reading area.
 */
@Composable
internal fun EditorPreviewReadingProgressBar(
    lazyListState: LazyListState,
    sourceRanges: List<MarkdownSourceRange>,
    contentLength: Int,
    modifier: Modifier = Modifier
) {
    val state by rememberPreviewReadingProgress(lazyListState, sourceRanges, contentLength)

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS)),
        exit = fadeOut(animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS)),
        modifier = modifier
    ) {
        VerticalScrollPositionTrack(
            // Track the scroll position directly without tweened animation
            // so the thumb follows the user's finger in real time.
            progress = state.progress,
            trackColor = trackColor,
            thumbColor = thumbColor,
            modifier = Modifier
                .fillMaxHeight()
                .width(ScrollTrackTouchWidth)
                .padding(vertical = ScrollTrackVerticalPadding)
                .semantics { contentDescription = "Reading progress" }
        )
    }
}

private val ScrollTrackWidth = 3.dp
private val ScrollTrackTouchWidth = 12.dp
private val ScrollTrackVerticalPadding = 8.dp
private val ScrollTrackCornerRadius = 2.dp
private val ScrollThumbMinHeight = 24.dp

/**
 * Custom-drawn vertical track with a rounded thumb. The [progress] value
 * (0 f..1 f) controls the thumb's vertical center position along the track.
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

        // Center the track horizontally within the touch target area.
        val trackLeft = (size.width - trackWidthPx) / 2f

        // Draw the background track.
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(trackLeft, 0f),
            size = Size(trackWidthPx, size.height),
            cornerRadius = cornerRadius
        )

        // Compute the thumb height and position.
        val thumbMinHeightPx = ScrollThumbMinHeight.toPx()
        val thumbHeight = thumbMinHeightPx.coerceAtMost(size.height)
        val maxThumbOffset = (size.height - thumbHeight).coerceAtLeast(0f)
        val thumbTop = maxThumbOffset * progress.coerceIn(0f, 1f)

        // Draw the thumb.
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(trackLeft, thumbTop),
            size = Size(trackWidthPx, thumbHeight),
            cornerRadius = cornerRadius
        )
    }
}

@Composable
private fun rememberPreviewReadingProgress(
    lazyListState: LazyListState,
    sourceRanges: List<MarkdownSourceRange>,
    contentLength: Int
): State<PreviewReadingProgressState> {
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
            PreviewReadingProgressState(
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

private data class PreviewReadingProgressState(
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
