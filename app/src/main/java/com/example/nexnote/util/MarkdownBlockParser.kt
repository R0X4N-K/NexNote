package com.example.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString

internal fun parseMarkdownBlocks(text: String, linkColor: Color): List<MarkdownBlock> =
    MarkdownBlockParsingSession(linkColor).parse(text)

private class MarkdownBlockParsingSession(
    private val linkColor: Color
) {
    private val blocks = mutableListOf<MarkdownBlock>()
    private val textLines = mutableListOf<String>()
    private val blockquoteLines = mutableListOf<String>()
    private val codeLines = mutableListOf<String>()
    private val tableLines = mutableListOf<String>()

    private var inCodeFence = false
    private var codeLang: String? = null
    private var fenceMarker = ""

    fun parse(text: String): List<MarkdownBlock> {
        if (text.isEmpty()) {
            return listOf(MarkdownBlock.TextBlock(AnnotatedString(text)))
        }

        text.split("\n").forEach(::consumeLine)
        flushRemaining()
        return blocks.ifEmpty { listOf(MarkdownBlock.TextBlock(AnnotatedString(text))) }
    }

    private fun consumeLine(line: String) {
        if (inCodeFence) {
            consumeCodeFenceLine(line)
            return
        }

        val isBlockquoteLine = line.isBlockquoteLine()
        val isTableLine = MarkdownPatterns.TABLE_LINE.matches(line.trim())
        flushBlockquoteBeforeNonQuote(isBlockquoteLine)
        flushTableBeforeNonTable(isTableLine)
        consumeNormalLine(line, isBlockquoteLine, isTableLine)
    }

    private fun consumeNormalLine(
        line: String,
        isBlockquoteLine: Boolean,
        isTableLine: Boolean
    ) {
        val codeFenceOpen = MarkdownPatterns.CODE_FENCE_OPEN.matchEntire(line)
        when {
            codeFenceOpen != null && !isBlockquoteLine -> startCodeFence(codeFenceOpen)
            isBlockquoteLine -> appendBlockquoteLine(line)
            MarkdownPatterns.HORIZONTAL_RULE.matches(line.trim()) -> appendHorizontalRule()
            MarkdownPatterns.STANDALONE_IMAGE_LINE.matchEntire(line) != null -> appendImageBlock(line)
            isTableLine -> appendTableLine(line)
            else -> textLines += line
        }
    }

    private fun consumeCodeFenceLine(line: String) {
        val closeMatch = MarkdownPatterns.CODE_FENCE_CLOSE.matchEntire(line)
        if (closeMatch != null && closesCurrentFence(closeMatch.groupValues[2])) {
            flushCode()
        } else {
            codeLines += line
        }
    }

    private fun startCodeFence(match: MatchResult) {
        fenceMarker = match.groupValues[2]
        codeLang = match.groupValues[3].trim().ifEmpty { null }
        flushText()
        inCodeFence = true
    }

    private fun appendBlockquoteLine(line: String) {
        flushText()
        blockquoteLines += if (line.startsWith("> ")) line.removePrefix("> ") else ""
    }

    private fun appendHorizontalRule() {
        flushText()
        blocks += MarkdownBlock.HorizontalRuleBlock
    }

    private fun appendImageBlock(line: String) {
        flushText()
        val match = MarkdownPatterns.STANDALONE_IMAGE_LINE.matchEntire(line) ?: return
        blocks += MarkdownBlock.ImageBlock(
            path = match.groupValues[2].trim(),
            altText = match.groupValues[1]
        )
    }

    private fun appendTableLine(line: String) {
        flushText()
        tableLines += line.trim()
    }

    private fun flushRemaining() {
        if (inCodeFence) flushCode()
        flushBlockquote()
        flushTable()
        flushText()
    }

    private fun flushText() {
        if (textLines.isEmpty()) return

        blocks += MarkdownBlock.TextBlock(
            MarkdownParser.parse(textLines.joinToString("\n"), linkColor)
        )
        textLines.clear()
    }

    private fun flushBlockquote() {
        if (blockquoteLines.isEmpty()) return

        blocks += MarkdownBlock.BlockquoteBlock(
            MarkdownParser.parse(blockquoteLines.joinToString("\n"), linkColor)
        )
        blockquoteLines.clear()
    }

    private fun flushCode() {
        blocks += MarkdownBlock.CodeBlock(
            code = codeLines.joinToString("\n"),
            language = codeLang
        )
        codeLines.clear()
        codeLang = null
        fenceMarker = ""
        inCodeFence = false
    }

    private fun flushTable() {
        if (tableLines.isEmpty()) return
        if (!hasValidTableSeparator()) {
            textLines.addAll(tableLines)
            tableLines.clear()
            return
        }

        flushText()
        val headers = parseTableRow(tableLines[0], linkColor)
        val alignments = parseSeparatorRow(tableLines[1], headers.size)
        val rows = tableLines.drop(2).map { parseTableRow(it, linkColor) }
        blocks += MarkdownBlock.TableBlock(headers, alignments, rows)
        tableLines.clear()
    }

    private fun hasValidTableSeparator(): Boolean =
        tableLines.size >= 2 &&
            MarkdownPatterns.TABLE_SEPARATOR_LINE.matches(tableLines[1].trim())

    private fun flushBlockquoteBeforeNonQuote(isBlockquoteLine: Boolean) {
        if (blockquoteLines.isNotEmpty() && !isBlockquoteLine) {
            flushBlockquote()
        }
    }

    private fun flushTableBeforeNonTable(isTableLine: Boolean) {
        if (tableLines.isNotEmpty() && !isTableLine) {
            flushTable()
        }
    }

    private fun closesCurrentFence(closingMarker: String): Boolean =
        closingMarker.first() == fenceMarker.first() &&
            closingMarker.length >= fenceMarker.length

    private fun String.isBlockquoteLine(): Boolean =
        startsWith("> ") || this == ">"
}
