package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.MarkdownBlock

/**
 * Renders the parsed markdown blocks inside a [LazyColumn].
 *
 * Only the blocks visible on screen are composed and measured, which keeps
 * scrolling smooth even for documents with hundreds of blocks. Each block
 * is keyed by its index so that Compose can efficiently diff items when the
 * content changes.
 */
@Composable
internal fun MarkdownPreviewContent(
    modifier: Modifier,
    lazyListState: LazyListState,
    state: MarkdownPreviewContentState,
    config: MarkdownPreviewContentConfig
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState
    ) {
        items(
            count = state.blocks.size,
            key = { index -> index }
        ) { index ->
            val block = state.blocks[index]
            val sourceRange = state.sourceRangeAt(index, config.markdown.length)
            RenderedMarkdownBlock(
                block       = block,
                sourceRange = sourceRange,
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
            table           = block,
            style           = config.style,
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
        annotatedText        = block.annotatedString,
        style                = config.style,
        markdown             = config.markdown,
        sourceRange          = sourceRange,
        highlightRanges      = config.highlightRanges,
        activeHighlightRange = config.activeHighlightRange,
        highlightColor       = state.highlightColor,
        onNoteLinkClick      = config.onNoteLinkClick
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
        content              = block.content,
        style                = config.style,
        markdown             = config.markdown,
        sourceRange          = sourceRange,
        highlightRanges      = config.highlightRanges,
        activeHighlightRange = config.activeHighlightRange,
        highlightColor       = state.highlightColor,
        onNoteLinkClick      = config.onNoteLinkClick
    )
}
