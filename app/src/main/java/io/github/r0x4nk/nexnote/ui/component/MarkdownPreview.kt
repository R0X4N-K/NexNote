package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.ui.theme.rememberContentMarkdownColors
import io.github.r0x4nk.nexnote.util.MarkdownBlock
import io.github.r0x4nk.nexnote.util.MarkdownColors
import io.github.r0x4nk.nexnote.util.MarkdownParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * Uses a [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] internally so
 * only the blocks visible on screen are composed and measured, keeping scroll
 * performance smooth even for very long notes.
 *
 * The parse result is memoised on [markdown] to avoid re-parsing on unrelated
 * recompositions.
 */
@Composable
fun MarkdownPreview(
    markdown: String,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    imageFileProvider: ((String) -> File)? = null,
    vaultImageByteProvider: (suspend (String) -> ByteArray?)? = null,
    highlightRanges: List<IntRange> = emptyList(),
    activeHighlightRange: IntRange? = null,
    contentBottomPadding: Dp = 0.dp,
    tableLayoutMode: TableLayoutMode = LocalMarkdownTableLayoutMode.current,
    onNoteLinkClick: (Long) -> Unit = {},
    onTaskListItemClick: ((markerOffset: Int) -> Unit)? = null
) {
    val contentState = rememberMarkdownPreviewContentState(markdown)
    val config = MarkdownPreviewContentConfig(
        markdown               = markdown,
        style                  = style,
        imageFileProvider      = imageFileProvider,
        vaultImageByteProvider = vaultImageByteProvider,
        highlightRanges        = highlightRanges,
        activeHighlightRange   = activeHighlightRange,
        contentBottomPadding   = contentBottomPadding,
        tableLayoutMode        = tableLayoutMode,
        onNoteLinkClick        = onNoteLinkClick,
        onTaskListItemClick    = onTaskListItemClick
    )

    MarkdownPreviewContent(
        modifier      = modifier,
        lazyListState = lazyListState,
        state         = contentState,
        config        = config
    )
}

internal class MarkdownPreviewContentConfig(
    val markdown: String,
    val style: TextStyle,
    val imageFileProvider: ((String) -> File)?,
    val vaultImageByteProvider: (suspend (String) -> ByteArray?)?,
    val highlightRanges: List<IntRange>,
    val activeHighlightRange: IntRange?,
    val contentBottomPadding: Dp,
    val tableLayoutMode: TableLayoutMode,
    val onNoteLinkClick: (Long) -> Unit,
    val onTaskListItemClick: ((markerOffset: Int) -> Unit)?
)

internal class MarkdownPreviewContentState(
    val blocks: List<MarkdownBlock>,
    val sourceRanges: List<MarkdownSourceRange>,
    val highlightColor: Color
)

@Composable
private fun rememberMarkdownPreviewContentState(markdown: String): MarkdownPreviewContentState {
    val colors = rememberContentMarkdownColors()
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    val sourceRanges = remember(markdown) { buildMarkdownBlockSourceRanges(markdown) }
    val blocks by rememberMarkdownBlocks(markdown = markdown, colors = colors)
    return MarkdownPreviewContentState(
        blocks         = blocks,
        sourceRanges   = sourceRanges,
        highlightColor = highlightColor
    )
}

@Composable
private fun rememberMarkdownBlocks(
    markdown: String,
    colors: MarkdownColors
): State<List<MarkdownBlock>> =
    produceState(
        initialValue = remember(markdown, colors) {
            MarkdownParser.getCached(markdown, colors) ?: emptyList()
        },
        key1 = markdown,
        key2 = colors
    ) {
        value = withContext(Dispatchers.Default) {
            MarkdownParser.parseBlocks(text = markdown, colors = colors)
        }
    }
