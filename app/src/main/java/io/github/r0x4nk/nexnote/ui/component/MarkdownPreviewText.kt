package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.r0x4nk.nexnote.util.NoteLinkMarkdown

/**
 * Text block renderer for markdown preview content.
 *
 * It preserves inline parser annotations, maps search highlights from markdown
 * source offsets onto the rendered text, and routes URL/note-link taps through
 * the same handler used by every text-like markdown block.
 */
@Composable
internal fun MarkdownTextBlock(
    annotatedText: AnnotatedString,
    style: TextStyle,
    markdown: String,
    sourceRange: MarkdownSourceRange,
    highlightRanges: List<IntRange>,
    activeHighlightRange: IntRange?,
    highlightColor: Color,
    onNoteLinkClick: (Long) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val displayText = rememberHighlightedPreviewText(
        annotatedText,
        markdown,
        sourceRange,
        highlightRanges,
        activeHighlightRange,
        highlightColor
    )

    BasicText(
        text = displayText,
        modifier = Modifier
            .fillMaxWidth()
            .markdownAnnotationTapHandler(
                displayText = displayText,
                getLayoutResult = { layoutResult },
                openUri = uriHandler::openUri,
                onNoteLinkClick = onNoteLinkClick
            ),
        style        = markdownTextStyle(style),
        onTextLayout = { layoutResult = it }
    )
}

/**
 * Blockquote renderer with a vertical accent bar and italicized content.
 *
 * The component shares highlight and link handling with normal text blocks so
 * search navigation and note links behave identically inside quoted text.
 */
@Composable
internal fun MarkdownBlockquote(
    content: AnnotatedString,
    style: TextStyle,
    markdown: String,
    sourceRange: MarkdownSourceRange,
    highlightRanges: List<IntRange>,
    activeHighlightRange: IntRange?,
    highlightColor: Color,
    onNoteLinkClick: (Long) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val displayText = rememberHighlightedPreviewText(
        content,
        markdown,
        sourceRange,
        highlightRanges,
        activeHighlightRange,
        highlightColor
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
    ) {
        BlockquoteBar(barColor)
        Spacer(Modifier.width(10.dp))
        BlockquoteText(
            displayText = displayText,
            style = style,
            openUri = uriHandler::openUri,
            onNoteLinkClick = onNoteLinkClick
        )
    }
}

@Composable
private fun rememberHighlightedPreviewText(
    annotatedText: AnnotatedString,
    markdown: String,
    sourceRange: MarkdownSourceRange,
    highlightRanges: List<IntRange>,
    activeHighlightRange: IntRange?,
    highlightColor: Color
): AnnotatedString =
    remember(annotatedText, markdown, sourceRange, highlightRanges, activeHighlightRange, highlightColor) {
        annotatedText.withPreviewHighlights(
            markdown             = markdown,
            sourceRange          = sourceRange,
            highlightRanges      = highlightRanges,
            activeHighlightRange = activeHighlightRange,
            highlightColor       = highlightColor
        )
    }

/**
 * Adds tap handling for links embedded in a rendered markdown [AnnotatedString].
 *
 * The parser emits note links with [NoteLinkMarkdown.ANNOTATION_TAG] and regular
 * URLs with the standard `URL` tag. Centralizing the hit-test here keeps text,
 * blockquotes, and table cells aligned on navigation behavior.
 */
internal fun Modifier.markdownAnnotationTapHandler(
    displayText: AnnotatedString,
    getLayoutResult: () -> TextLayoutResult?,
    openUri: (String) -> Unit,
    onNoteLinkClick: (Long) -> Unit
): Modifier =
    pointerInput(displayText, openUri, onNoteLinkClick) {
        detectTapGestures { offset ->
            val layout = getLayoutResult() ?: return@detectTapGestures
            val position = layout.getOffsetForPosition(offset)
            val noteId = displayText
                .getStringAnnotations(
                    tag = NoteLinkMarkdown.ANNOTATION_TAG,
                    start = position,
                    end = position
                )
                .firstOrNull()
                ?.item
                ?.toLongOrNull()
            if (noteId != null) {
                onNoteLinkClick(noteId)
                return@detectTapGestures
            }

            displayText
                .getStringAnnotations(tag = "URL", start = position, end = position)
                .firstOrNull()
                ?.let { annotation ->
                    if (isSupportedMarkdownLink(annotation.item)) {
                        runCatching { openUri(annotation.item) }
                    }
                }
        }
    }

@Composable
private fun markdownTextStyle(style: TextStyle): TextStyle =
    style.copy(color = MaterialTheme.colorScheme.onSurface)

@Composable
private fun BlockquoteBar(color: Color) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight()
            .drawBehind { drawRect(color) }
    )
}

@Composable
private fun RowScope.BlockquoteText(
    displayText: AnnotatedString,
    style: TextStyle,
    openUri: (String) -> Unit,
    onNoteLinkClick: (Long) -> Unit
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = displayText,
        style = blockquoteTextStyle(style),
        onTextLayout = { layoutResult = it },
        modifier = Modifier
            .weight(1f)
            .markdownAnnotationTapHandler(
                displayText = displayText,
                getLayoutResult = { layoutResult },
                openUri = openUri,
                onNoteLinkClick = onNoteLinkClick
            )
    )
}

@Composable
private fun blockquoteTextStyle(style: TextStyle): TextStyle =
    style.copy(
        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        fontStyle = FontStyle.Italic
    )

/**
 * Horizontally scrollable code block for markdown preview.
 *
 * Code keeps a monospace face and its own rounded background so long lines can
 * remain intact without forcing the full note preview wider than the viewport.
 */
@Composable
internal fun MarkdownCodeBlock(code: String) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .drawBehind { drawRect(bgColor) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                text  = code,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 13.sp,
                    color      = textColor
                )
            )
        }
    }
}

private fun AnnotatedString.withPreviewHighlights(
    markdown: String,
    sourceRange: MarkdownSourceRange,
    highlightRanges: List<IntRange>,
    activeHighlightRange: IntRange?,
    highlightColor: Color
): AnnotatedString {
    if (highlightRanges.isEmpty() && activeHighlightRange == null) return this

    return buildAnnotatedString {
        append(this@withPreviewHighlights)
        highlightRanges.forEach { range ->
            findRenderedHighlightRange(
                markdown = markdown,
                renderedText = text,
                sourceRange = sourceRange,
                sourceRangeToHighlight = range
            )?.let { renderedRange ->
                addPreviewHighlight(renderedRange, highlightColor.copy(alpha = 0.55f))
            }
        }
        activeHighlightRange?.let { range ->
            findRenderedHighlightRange(
                markdown = markdown,
                renderedText = text,
                sourceRange = sourceRange,
                sourceRangeToHighlight = range
            )?.let { renderedRange ->
                addPreviewHighlight(renderedRange, highlightColor)
            }
        }
    }
}

private fun AnnotatedString.Builder.addPreviewHighlight(
    range: IntRange,
    color: Color
) {
    addStyle(
        style = SpanStyle(background = color),
        start = range.first,
        end = range.last + 1
    )
}

private fun findRenderedHighlightRange(
    markdown: String,
    renderedText: String,
    sourceRange: MarkdownSourceRange,
    sourceRangeToHighlight: IntRange
): IntRange? {
    val highlightStart = sourceRangeToHighlight.first.coerceIn(0, markdown.length)
    val highlightEnd = (sourceRangeToHighlight.last + 1).coerceIn(highlightStart, markdown.length)
    if (highlightStart < sourceRange.start || highlightStart >= sourceRange.end) return null
    if (highlightEnd <= highlightStart) return null

    val token = markdown.substring(highlightStart, highlightEnd)
    if (token.isBlank()) return null

    val occurrenceInSource = countMatchesBefore(
        text = markdown,
        token = token,
        start = sourceRange.start,
        end = highlightStart
    )

    return findMatchAtOccurrence(
        text = renderedText,
        token = token,
        occurrence = occurrenceInSource
    )
}

private fun countMatchesBefore(
    text: String,
    token: String,
    start: Int,
    end: Int
): Int {
    var count = 0
    var searchStart = start.coerceIn(0, text.length)
    val safeEnd = end.coerceIn(searchStart, text.length)

    while (searchStart <= safeEnd - token.length) {
        val foundIndex = text.indexOf(token, startIndex = searchStart, ignoreCase = true)
        if (foundIndex == -1 || foundIndex >= safeEnd) break
        count += 1
        searchStart = foundIndex + 1
    }

    return count
}

private fun findMatchAtOccurrence(
    text: String,
    token: String,
    occurrence: Int
): IntRange? {
    var seen = 0
    var searchStart = 0

    while (searchStart <= text.length - token.length) {
        val foundIndex = text.indexOf(token, startIndex = searchStart, ignoreCase = true)
        if (foundIndex == -1) break
        if (seen == occurrence) return foundIndex..<foundIndex + token.length
        seen += 1
        searchStart = foundIndex + 1
    }

    return null
}
