package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import io.github.r0x4nk.nexnote.util.MarkdownBlock
import io.github.r0x4nk.nexnote.util.MarkdownParser

private const val NOTE_CARD_BLOCK_SEPARATOR = "\n"
private const val NOTE_CARD_TABLE_SEPARATOR = " | "
private const val NOTE_CARD_IMAGE_PLACEHOLDER = "Image"

internal fun buildNoteCardDisplayText(
    sourceText: String,
    ranges: List<IntRange>,
    linkColor: Color,
    highlightColor: Color,
    renderMarkdown: Boolean
): AnnotatedString {
    val renderedText = if (renderMarkdown) {
        renderCompactMarkdown(sourceText, linkColor)
    } else {
        AnnotatedString(sourceText)
    }

    return renderedText.withNoteCardHighlights(
        sourceText = sourceText,
        ranges = ranges,
        highlightColor = highlightColor
    )
}

private fun renderCompactMarkdown(
    sourceText: String,
    linkColor: Color
): AnnotatedString {
    val renderedText = buildAnnotatedString {
        MarkdownParser.parseBlocks(sourceText, linkColor).forEach { block ->
            appendCompactMarkdownBlock(block)
        }
    }

    return if (renderedText.text.isBlank() && sourceText.isNotBlank()) {
        AnnotatedString(sourceText)
    } else {
        renderedText
    }
}

private fun AnnotatedString.Builder.appendCompactMarkdownBlock(block: MarkdownBlock) {
    when (block) {
        is MarkdownBlock.TextBlock -> appendCompactText(block.annotatedString)
        is MarkdownBlock.BlockquoteBlock -> appendCompactBlockquote(block.content)
        is MarkdownBlock.CodeBlock -> appendCompactCode(block.code)
        is MarkdownBlock.ImageBlock -> appendCompactImage(block.altText)
        is MarkdownBlock.TableBlock -> appendCompactTable(block)
        MarkdownBlock.HorizontalRuleBlock -> Unit
    }
}

private fun AnnotatedString.Builder.appendCompactText(text: AnnotatedString) {
    appendBlockSeparatorIfNeeded()
    appendCardInlineText(text)
}

private fun AnnotatedString.Builder.appendCompactBlockquote(content: AnnotatedString) {
    appendBlockSeparatorIfNeeded()
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
        appendCardInlineText(content)
    }
}

private fun AnnotatedString.Builder.appendCompactCode(code: String) {
    if (code.isBlank()) return

    appendBlockSeparatorIfNeeded()
    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
        append(code)
    }
}

private fun AnnotatedString.Builder.appendCompactImage(altText: String) {
    appendBlockSeparatorIfNeeded()
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
        append(altText.ifBlank { NOTE_CARD_IMAGE_PLACEHOLDER })
    }
}

private fun AnnotatedString.Builder.appendCompactTable(table: MarkdownBlock.TableBlock) {
    if (table.headers.isEmpty()) return

    appendBlockSeparatorIfNeeded()
    appendCompactTableRow(table.headers)
    table.rows.firstOrNull()?.let { firstRow ->
        append(NOTE_CARD_BLOCK_SEPARATOR)
        appendCompactTableRow(firstRow)
    }
}

private fun AnnotatedString.Builder.appendCompactTableRow(cells: List<AnnotatedString>) {
    cells.forEachIndexed { index, cell ->
        if (index > 0) append(NOTE_CARD_TABLE_SEPARATOR)
        appendCardInlineText(cell)
    }
}

private fun AnnotatedString.Builder.appendBlockSeparatorIfNeeded() {
    if (length > 0) append(NOTE_CARD_BLOCK_SEPARATOR)
}

private fun AnnotatedString.Builder.appendCardInlineText(text: AnnotatedString) {
    val startOffset = length
    append(text.text)
    text.spanStyles.forEach { range ->
        addStyle(
            style = range.item.toCardInlineStyle(),
            start = startOffset + range.start,
            end = startOffset + range.end
        )
    }
}

private fun SpanStyle.toCardInlineStyle(): SpanStyle =
    copy(
        fontSize = TextUnit.Unspecified,
        letterSpacing = TextUnit.Unspecified
    )

private fun AnnotatedString.withNoteCardHighlights(
    sourceText: String,
    ranges: List<IntRange>,
    highlightColor: Color
): AnnotatedString {
    if (ranges.isEmpty()) return this

    return buildAnnotatedString {
        append(this@withNoteCardHighlights)
        ranges.forEach { range ->
            findRenderedNoteCardRange(
                sourceText = sourceText,
                renderedText = text,
                sourceRange = range
            )?.let { renderedRange ->
                addNoteCardHighlight(renderedRange, highlightColor)
            }
        }
    }
}

private fun AnnotatedString.Builder.addNoteCardHighlight(
    range: IntRange,
    highlightColor: Color
) {
    addStyle(
        SpanStyle(background = highlightColor.copy(alpha = 0.25f)),
        start = range.first,
        end = range.last + 1
    )
}

private fun findRenderedNoteCardRange(
    sourceText: String,
    renderedText: String,
    sourceRange: IntRange
): IntRange? {
    val safeStart = sourceRange.first.coerceIn(0, sourceText.length)
    val safeEnd = (sourceRange.last + 1).coerceIn(safeStart, sourceText.length)
    if (safeStart >= safeEnd) return null

    if (sourceText == renderedText) return safeStart..<safeEnd

    val token = sourceText.substring(safeStart, safeEnd)
    if (token.isBlank()) return null

    val occurrence = countMatchesBefore(
        text = sourceText,
        token = token,
        endExclusive = safeStart
    )
    return findMatchAtOccurrence(
        text = renderedText,
        token = token,
        occurrence = occurrence
    )
}

private fun countMatchesBefore(
    text: String,
    token: String,
    endExclusive: Int
): Int {
    var count = 0
    var searchStart = 0
    val safeEnd = endExclusive.coerceIn(0, text.length)

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
