package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import io.github.r0x4nk.nexnote.ui.component.MarkdownSourceRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private const val PREVIEW_ITEM_LAYOUT_TIMEOUT_MS = 500L

/**
 * Returns the block index whose source range contains [sourceOffset].
 *
 * Falls back to the nearest preceding block when the offset sits between
 * ranges, or to the last block when the cursor is at the end of the document.
 */
internal fun List<MarkdownSourceRange>.blockIndexForSourceOffset(sourceOffset: Int): Int {
    if (isEmpty()) return 0
    val safeOffset = sourceOffset.coerceAtLeast(0)
    val index = indexOfFirst { safeOffset >= it.start && safeOffset < it.end }
    if (index >= 0) return index

    val precedingIndex = indexOfLast { safeOffset >= it.start }
    return if (precedingIndex >= 0) precedingIndex else 0
}

/**
 * Maps the requested viewport anchor in the preview [LazyListState] back to a
 * source offset. This preserves mode-toggle position without composing the
 * whole markdown preview just to measure every block.
 */
internal fun sourceOffsetForPreviewViewportAnchor(
    sourceRanges: List<MarkdownSourceRange>,
    layoutInfo: LazyListLayoutInfo,
    viewportFraction: Float
): Int {
    if (sourceRanges.isEmpty()) return 0
    val anchorY = layoutInfo.viewportAnchorY(viewportFraction)
    val itemInfo = layoutInfo.visibleItemClosestTo(anchorY) ?: return sourceRanges.first().start
    val range = sourceRanges.sourceRangeForItem(itemInfo.index)
    return range.offsetAt(itemInfo.progressAt(anchorY))
}

/**
 * Scrolls a lazy preview so the source offset lands near [viewportFraction].
 *
 * The item is first brought into view when necessary, then its measured height
 * is used to compute the intra-block scroll offset. This restores the old
 * source-to-pixel behavior while keeping long previews virtualized.
 */
internal suspend fun LazyListState.scrollToSourceOffset(
    sourceRanges: List<MarkdownSourceRange>,
    sourceOffset: Int,
    viewportFraction: Float,
    animated: Boolean
) {
    if (sourceRanges.isEmpty()) return

    val totalItems = awaitTotalItems()
    if (totalItems <= 0) return

    val itemIndex = sourceRanges
        .blockIndexForSourceOffset(sourceOffset)
        .coerceIn(0, totalItems - 1)

    val itemInfo = visibleItemInfo(itemIndex)
        ?: bringItemIntoView(itemIndex, animated)
        ?: return

    val viewportHeight = layoutInfo.viewportHeight()
    val scrollOffset = previewItemScrollOffsetForSourceOffset(
        sourceRanges     = sourceRanges,
        itemIndex        = itemIndex,
        sourceOffset     = sourceOffset,
        itemHeight       = itemInfo.size,
        viewportHeight   = viewportHeight,
        viewportFraction = viewportFraction
    )

    if (animated) {
        animateScrollToItem(itemIndex, scrollOffset)
    } else {
        scrollToItem(itemIndex, scrollOffset)
    }
}

internal suspend fun LazyListState.animateScrollToPreviewTop() {
    if (awaitTotalItems() > 0) {
        animateScrollToItem(index = 0)
    }
}

internal suspend fun LazyListState.animateScrollToPreviewBottom() {
    val totalItems = awaitTotalItems()
    if (totalItems <= 0) return

    val lastIndex = totalItems - 1
    animateScrollToItem(lastIndex)

    val itemInfo = awaitVisibleItemInfo(lastIndex) ?: return
    val bottomAlignedOffset = (itemInfo.size - layoutInfo.viewportHeight()).coerceAtLeast(0)
    animateScrollToItem(lastIndex, bottomAlignedOffset)
}

internal fun previewItemScrollOffsetForSourceOffset(
    sourceRanges: List<MarkdownSourceRange>,
    itemIndex: Int,
    sourceOffset: Int,
    itemHeight: Int,
    viewportHeight: Int,
    viewportFraction: Float
): Int {
    val range = sourceRanges.sourceRangeForItem(itemIndex)
    val targetY = itemHeight.coerceAtLeast(1) * range.progressAt(sourceOffset)
    return (targetY - viewportHeight.coerceAtLeast(1) * viewportFraction).roundToInt()
}

private suspend fun LazyListState.bringItemIntoView(
    itemIndex: Int,
    animated: Boolean
): LazyListItemInfo? {
    if (animated) {
        animateScrollToItem(itemIndex)
    } else {
        scrollToItem(itemIndex)
    }
    return awaitVisibleItemInfo(itemIndex)
}

private suspend fun LazyListState.awaitTotalItems(): Int {
    layoutInfo.totalItemsCount.takeIf { it > 0 }?.let { return it }
    return withTimeoutOrNull(PREVIEW_ITEM_LAYOUT_TIMEOUT_MS) {
        snapshotFlow { layoutInfo.totalItemsCount }
            .first { it > 0 }
    } ?: 0
}

private suspend fun LazyListState.awaitVisibleItemInfo(itemIndex: Int): LazyListItemInfo? {
    visibleItemInfo(itemIndex)?.let { return it }
    return withTimeoutOrNull(PREVIEW_ITEM_LAYOUT_TIMEOUT_MS) {
        snapshotFlow { visibleItemInfo(itemIndex) }
            .first { it != null }
    }
}

private fun LazyListState.visibleItemInfo(itemIndex: Int): LazyListItemInfo? =
    layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }

private fun LazyListLayoutInfo.viewportAnchorY(viewportFraction: Float): Float {
    val fraction = viewportFraction.coerceIn(0f, 1f)
    return viewportStartOffset + viewportHeight() * fraction
}

private fun LazyListLayoutInfo.viewportHeight(): Int =
    (viewportEndOffset - viewportStartOffset).coerceAtLeast(1)

private fun LazyListLayoutInfo.visibleItemClosestTo(anchorY: Float): LazyListItemInfo? {
    return visibleItemsInfo.minByOrNull { item ->
        when {
            anchorY < item.offset -> item.offset - anchorY
            anchorY > item.offset + item.size -> anchorY - (item.offset + item.size)
            else -> 0f
        }
    }
}

private fun List<MarkdownSourceRange>.sourceRangeForItem(itemIndex: Int): MarkdownSourceRange =
    getOrElse(itemIndex) { last() }

private fun MarkdownSourceRange.progressAt(sourceOffset: Int): Float {
    val sourceLength = (end - start).coerceAtLeast(1)
    return ((sourceOffset - start).toFloat() / sourceLength).coerceIn(0f, 1f)
}

private fun MarkdownSourceRange.offsetAt(progress: Float): Int {
    val sourceLength = (end - start).coerceAtLeast(1)
    return (start + sourceLength * progress.coerceIn(0f, 1f))
        .roundToInt()
        .coerceIn(start, end)
}

private fun LazyListItemInfo.progressAt(anchorY: Float): Float =
    ((anchorY - offset) / size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
