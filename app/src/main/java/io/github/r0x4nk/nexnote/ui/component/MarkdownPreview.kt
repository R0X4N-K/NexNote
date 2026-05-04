package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import io.github.r0x4nk.nexnote.util.MarkdownBlock
import io.github.r0x4nk.nexnote.util.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bounds of a rendered preview block mapped back to its Markdown source range.
 * [sourceEnd] is exclusive; [top] and [bottom] are local to the preview content.
 */
data class MarkdownPreviewBlockLayout(
    val sourceStart: Int,
    val sourceEnd: Int,
    val top: Float,
    val bottom: Float
) {
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
    val sourceLength: Int get() = (sourceEnd - sourceStart).coerceAtLeast(1)

    fun containsSourceOffset(offset: Int): Boolean =
        offset >= sourceStart && offset < sourceEnd
}

/**
 * Renders [markdown] as formatted content using the custom parser.
 *
 * Content is split into blocks ([MarkdownBlock]) and each block type is
 * rendered by its own composable:
 * - [MarkdownBlock.TextBlock] -> inline-styled text with clickable links
 * - [MarkdownBlock.ImageBlock] -> async local image
 * - [MarkdownBlock.HorizontalRuleBlock] -> horizontal divider
 * - [MarkdownBlock.BlockquoteBlock] -> indented block with a coloured left bar
 * - [MarkdownBlock.CodeBlock] -> monospace code block with background
 * - [MarkdownBlock.TableBlock] -> GFM pipe table with header + data rows
 *
 * The parse result is memoised on [markdown] to avoid re-parsing on unrelated
 * recompositions.
 */
@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    imageFileProvider: ((String) -> File)? = null,
    highlightRanges: List<IntRange> = emptyList(),
    activeHighlightRange: IntRange? = null,
    onNoteLinkClick: (Long) -> Unit = {},
    onSourceLayoutsChange: (List<MarkdownPreviewBlockLayout>) -> Unit = {}
) {
    val contentState = rememberMarkdownPreviewContentState(markdown)
    val config = MarkdownPreviewContentConfig(
        markdown              = markdown,
        style                 = style,
        imageFileProvider     = imageFileProvider,
        highlightRanges       = highlightRanges,
        activeHighlightRange  = activeHighlightRange,
        onNoteLinkClick       = onNoteLinkClick,
        onSourceLayoutsChange = onSourceLayoutsChange
    )

    ResetMeasuredLayouts(
        markdown              = markdown,
        blocks                = contentState.blocks,
        blockLayouts          = contentState.blockLayouts,
        onSourceLayoutsChange = onSourceLayoutsChange
    )

    MarkdownPreviewContent(modifier = modifier, state = contentState, config = config)
}

internal class MarkdownPreviewContentConfig(
    val markdown: String,
    val style: TextStyle,
    val imageFileProvider: ((String) -> File)?,
    val highlightRanges: List<IntRange>,
    val activeHighlightRange: IntRange?,
    val onNoteLinkClick: (Long) -> Unit,
    val onSourceLayoutsChange: (List<MarkdownPreviewBlockLayout>) -> Unit
)

internal class MarkdownPreviewContentState(
    val blocks: List<MarkdownBlock>,
    val sourceRanges: List<MarkdownSourceRange>,
    val blockLayouts: MutableMap<Int, MarkdownPreviewBlockLayout>,
    val previewTopInRoot: FloatArray,
    val highlightColor: Color
)

@Composable
private fun rememberMarkdownPreviewContentState(markdown: String): MarkdownPreviewContentState {
    val linkColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    val sourceRanges = remember(markdown) { buildMarkdownBlockSourceRanges(markdown) }
    val blockLayouts = remember(markdown) { mutableStateMapOf<Int, MarkdownPreviewBlockLayout>() }
    val previewTopInRoot = remember(markdown) { FloatArray(1) }
    val blocks by rememberMarkdownBlocks(markdown = markdown, linkColor = linkColor)
    return MarkdownPreviewContentState(
        blocks           = blocks,
        sourceRanges     = sourceRanges,
        blockLayouts     = blockLayouts,
        previewTopInRoot = previewTopInRoot,
        highlightColor   = highlightColor
    )
}

@Composable
private fun ResetMeasuredLayouts(
    markdown: String,
    blocks: List<MarkdownBlock>,
    blockLayouts: MutableMap<Int, MarkdownPreviewBlockLayout>,
    onSourceLayoutsChange: (List<MarkdownPreviewBlockLayout>) -> Unit
) {
    LaunchedEffect(markdown, blocks) {
        blockLayouts.clear()
        onSourceLayoutsChange(emptyList())
    }
}

@Composable
private fun rememberMarkdownBlocks(
    markdown: String,
    linkColor: Color
): State<List<MarkdownBlock>> =
    produceState(
        initialValue = remember(markdown, linkColor) {
            MarkdownParser.getCached(markdown, linkColor) ?: emptyList()
        },
        key1 = markdown,
        key2 = linkColor
    ) {
        value = withContext(Dispatchers.Default) {
            MarkdownParser.parseBlocks(text = markdown, linkColor = linkColor)
        }
    }
