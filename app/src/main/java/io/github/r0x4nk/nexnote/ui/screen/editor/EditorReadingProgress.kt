package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.MarkdownSourceRange

private const val READING_PROGRESS_ANIMATION_MS = 120
private const val READING_PROGRESS_ANCHOR_FRACTION = 0.35f

@Composable
internal fun EditorPreviewReadingProgressBar(
    lazyListState: LazyListState,
    sourceRanges: List<MarkdownSourceRange>,
    contentLength: Int,
    modifier: Modifier = Modifier
) {
    val state by rememberPreviewReadingProgress(lazyListState, sourceRanges, contentLength)
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS),
        label = "preview_reading_progress"
    )

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS)),
        exit = fadeOut(animationSpec = tween(durationMillis = READING_PROGRESS_ANIMATION_MS)),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .semantics { contentDescription = "Reading progress" },
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
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
