package com.example.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.nexnote.util.MarkdownBlock

@Composable
internal fun MarkdownPreviewContent(
    modifier: Modifier,
    state: MarkdownPreviewContentState,
    config: MarkdownPreviewContentConfig
) {
    Column(modifier = modifier.trackPreviewTop(state.previewTopInRoot)) {
        state.blocks.forEachIndexed { index, block ->
            MeasuredMarkdownBlock(
                index       = index,
                block       = block,
                sourceRange = state.sourceRangeAt(index, config.markdown.length),
                state       = state,
                config      = config
            )
        }
    }
}

private fun MarkdownPreviewContentState.sourceRangeAt(
    index: Int,
    markdownLength: Int
): MarkdownSourceRange =
    sourceRanges.getOrElse(index) { MarkdownSourceRange(start = 0, end = markdownLength) }

private fun Modifier.trackPreviewTop(previewTopInRoot: FloatArray): Modifier =
    onGloballyPositioned { coordinates ->
        previewTopInRoot[0] = coordinates.positionInRoot().y
    }

@Composable
private fun MeasuredMarkdownBlock(
    index: Int,
    block: MarkdownBlock,
    sourceRange: MarkdownSourceRange,
    state: MarkdownPreviewContentState,
    config: MarkdownPreviewContentConfig
) {
    Box(
        modifier = Modifier.measureMarkdownBlock(
            index                 = index,
            markdownLength        = config.markdown.length,
            sourceRange           = sourceRange,
            state                 = state,
            onSourceLayoutsChange = config.onSourceLayoutsChange
        )
    ) {
        RenderedMarkdownBlock(block = block, sourceRange = sourceRange, state = state, config = config)
    }
}

private fun Modifier.measureMarkdownBlock(
    index: Int,
    markdownLength: Int,
    sourceRange: MarkdownSourceRange,
    state: MarkdownPreviewContentState,
    onSourceLayoutsChange: (List<MarkdownPreviewBlockLayout>) -> Unit
): Modifier =
    onGloballyPositioned { coordinates ->
        val top = coordinates.positionInRoot().y - state.previewTopInRoot[0]
        val newLayout = sourceRange.toPreviewBlockLayout(
            markdownLength = markdownLength,
            top            = top,
            bottom         = top + coordinates.size.height.toFloat()
        )
        state.blockLayouts.updateLayout(index, newLayout, onSourceLayoutsChange)
    }

@Composable
private fun RenderedMarkdownBlock(
    block: MarkdownBlock,
    sourceRange: MarkdownSourceRange,
    state: MarkdownPreviewContentState,
    config: MarkdownPreviewContentConfig
) {
    when (block) {
        is MarkdownBlock.TextBlock -> RenderMarkdownTextBlock(block, sourceRange, state, config)
        is MarkdownBlock.ImageBlock -> RenderMarkdownImageBlock(block, config)
        MarkdownBlock.HorizontalRuleBlock -> MarkdownHorizontalRule()
        is MarkdownBlock.BlockquoteBlock -> RenderMarkdownBlockquote(block, sourceRange, state, config)
        is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(code = block.code)
        is MarkdownBlock.TableBlock -> MarkdownTableBlock(
            table = block,
            style = config.style,
            onNoteLinkClick = config.onNoteLinkClick
        )
    }
}

@Composable
private fun RenderMarkdownTextBlock(
    block: MarkdownBlock.TextBlock,
    sourceRange: MarkdownSourceRange,
    state: MarkdownPreviewContentState,
    config: MarkdownPreviewContentConfig
) {
    MarkdownTextBlock(
        annotatedText  = block.annotatedString,
        style          = config.style,
        markdown       = config.markdown,
        sourceRange    = sourceRange,
        highlightRanges = config.highlightRanges,
        activeHighlightRange = config.activeHighlightRange,
        highlightColor = state.highlightColor,
        onNoteLinkClick = config.onNoteLinkClick
    )
}

@Composable
private fun RenderMarkdownImageBlock(
    block: MarkdownBlock.ImageBlock,
    config: MarkdownPreviewContentConfig
) {
    MarkdownImageBlock(
        imageFileProvider = config.imageFileProvider,
        relativePath      = block.path,
        altText           = block.altText
    )
}

@Composable
private fun MarkdownHorizontalRule() {
    HorizontalDivider(
        modifier  = Modifier.padding(vertical = 8.dp),
        thickness = 1.dp,
        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    )
}

@Composable
private fun RenderMarkdownBlockquote(
    block: MarkdownBlock.BlockquoteBlock,
    sourceRange: MarkdownSourceRange,
    state: MarkdownPreviewContentState,
    config: MarkdownPreviewContentConfig
) {
    MarkdownBlockquote(
        content        = block.content,
        style          = config.style,
        markdown       = config.markdown,
        sourceRange    = sourceRange,
        highlightRanges = config.highlightRanges,
        activeHighlightRange = config.activeHighlightRange,
        highlightColor = state.highlightColor,
        onNoteLinkClick = config.onNoteLinkClick
    )
}

private fun MarkdownSourceRange.toPreviewBlockLayout(
    markdownLength: Int,
    top: Float,
    bottom: Float
): MarkdownPreviewBlockLayout {
    val safeSourceStart = start.coerceIn(0, markdownLength)
    val safeSourceEnd = end.coerceIn(safeSourceStart, markdownLength)
    return MarkdownPreviewBlockLayout(
        sourceStart = safeSourceStart,
        sourceEnd   = safeSourceEnd,
        top         = top,
        bottom      = bottom
    )
}

private fun MutableMap<Int, MarkdownPreviewBlockLayout>.updateLayout(
    index: Int,
    newLayout: MarkdownPreviewBlockLayout,
    onSourceLayoutsChange: (List<MarkdownPreviewBlockLayout>) -> Unit
) {
    if (this[index]?.isSameLayoutAs(newLayout) == true) return

    this[index] = newLayout
    onSourceLayoutsChange(values.sortedBy { it.top })
}

private fun MarkdownPreviewBlockLayout.isSameLayoutAs(other: MarkdownPreviewBlockLayout): Boolean =
    sourceStart == other.sourceStart &&
        sourceEnd == other.sourceEnd &&
        top.isCloseTo(other.top) &&
        bottom.isCloseTo(other.bottom)

private fun Float.isCloseTo(other: Float): Boolean =
    this >= other - 0.5f && this <= other + 0.5f
