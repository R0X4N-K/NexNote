package com.example.nexnote.ui.component

internal data class MarkdownSourceRange(
    val start: Int,
    val end: Int
)

private const val MAX_SOURCE_RANGE_CACHE_ENTRIES = 30

private val sourceRangeCache = object : LinkedHashMap<String, List<MarkdownSourceRange>>(
    MAX_SOURCE_RANGE_CACHE_ENTRIES,
    0.75f,
    true
) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<String, List<MarkdownSourceRange>>?
    ): Boolean = size > MAX_SOURCE_RANGE_CACHE_ENTRIES
}

private val previewStandaloneImageLine = Regex("""^\s*!\[([^\]]*?)]\(([^)]+?)\)\s*$""")
private val previewHorizontalRule = Regex("""^\s*([-*_])\s*(?:\1\s*){2,}$""")
private val previewCodeFenceOpen = Regex("""^(\s*)(```+|~~~+)(.*)$""")
private val previewCodeFenceClose = Regex("""^(\s*)(```+|~~~+)\s*$""")
private val previewTableLine = Regex("""^\s*\|.+\|\s*$""")
private val previewTableSeparatorLine = Regex("""^\s*\|(\s*:?-+:?\s*\|)+\s*$""")

internal fun buildMarkdownBlockSourceRanges(markdown: String): List<MarkdownSourceRange> {
    synchronized(sourceRangeCache) {
        sourceRangeCache[markdown]
    }?.let { return it }

    val result = if (markdown.isEmpty()) {
        listOf(MarkdownSourceRange(start = 0, end = 0))
    } else {
        MarkdownSourceRangeBuilder(markdown).build()
    }

    synchronized(sourceRangeCache) {
        sourceRangeCache[markdown] = result
    }
    return result
}

private class MarkdownSourceRangeBuilder(
    private val markdown: String
) {
    private val lines = markdown.split("\n")
    private val lineStarts = buildLineStarts(lines)
    private val ranges = mutableListOf<MarkdownSourceRange>()
    private var textStart: Int? = null
    private var textEnd = 0
    private var blockquoteStart: Int? = null
    private var blockquoteEnd = 0
    private var tableStart: Int? = null
    private var tableEnd = 0
    private var tableLineCount = 0
    private var tableSeparatorValid = false
    private var inCodeFence = false
    private var codeStart = 0
    private var fenceMarker = ""

    fun build(): List<MarkdownSourceRange> {
        lines.forEachIndexed { index, line -> processLine(index, line) }
        finishOpenBlocks()
        return ranges.ifEmpty { listOf(MarkdownSourceRange(start = 0, end = markdown.length)) }
    }

    private fun processLine(index: Int, line: String) {
        val start = lineStarts[index]
        val end = lineEnd(index)
        if (handleCodeFenceContent(line, end)) return

        val isBlockquoteLine = line.startsWith("> ") || line == ">"
        val isTableLine = previewTableLine.matches(line.trim())
        if (blockquoteStart != null && !isBlockquoteLine) flushBlockquote()
        if (tableStart != null && !isTableLine) flushTable()

        processRegularLine(
            line            = line,
            start           = start,
            end             = end,
            isBlockquoteLine = isBlockquoteLine,
            isTableLine     = isTableLine
        )
    }

    private fun processRegularLine(
        line: String,
        start: Int,
        end: Int,
        isBlockquoteLine: Boolean,
        isTableLine: Boolean
    ) {
        val codeFenceMatch = previewCodeFenceOpen.matchEntire(line)
        when {
            codeFenceMatch != null && !isBlockquoteLine -> openCodeFence(start, codeFenceMatch)
            isBlockquoteLine -> addBlockquoteLine(start, end)
            previewHorizontalRule.matches(line.trim()) -> addSingleBlockRange(start, end)
            previewStandaloneImageLine.matchEntire(line) != null -> addSingleBlockRange(start, end)
            isTableLine -> addTableLine(line, start, end)
            else -> addTextRange(start, end)
        }
    }

    private fun handleCodeFenceContent(line: String, end: Int): Boolean {
        if (!inCodeFence) return false

        val closeMatch = previewCodeFenceClose.matchEntire(line)
        val closingMarker = closeMatch?.groupValues?.get(2)
        if (closingMarker != null &&
            closingMarker.first() == fenceMarker.first() &&
            closingMarker.length >= fenceMarker.length
        ) {
            ranges += MarkdownSourceRange(start = codeStart, end = end)
            inCodeFence = false
            fenceMarker = ""
        }
        return true
    }

    private fun openCodeFence(start: Int, match: MatchResult) {
        flushText()
        inCodeFence = true
        codeStart = start
        fenceMarker = match.groupValues[2]
    }

    private fun addBlockquoteLine(start: Int, end: Int) {
        flushText()
        if (blockquoteStart == null) blockquoteStart = start
        blockquoteEnd = end
    }

    private fun addSingleBlockRange(start: Int, end: Int) {
        flushText()
        ranges += MarkdownSourceRange(start = start, end = end)
    }

    private fun addTableLine(line: String, start: Int, end: Int) {
        flushText()
        if (tableStart == null) tableStart = start
        tableEnd = end
        tableLineCount += 1
        if (tableLineCount == 2) {
            tableSeparatorValid = previewTableSeparatorLine.matches(line.trim())
        }
    }

    private fun addTextRange(start: Int, end: Int) {
        if (textStart == null) textStart = start
        textEnd = end
    }

    private fun flushText() {
        textStart?.let { start ->
            ranges += MarkdownSourceRange(start = start, end = textEnd.coerceAtLeast(start))
        }
        textStart = null
        textEnd = 0
    }

    private fun flushBlockquote() {
        blockquoteStart?.let { start ->
            ranges += MarkdownSourceRange(start = start, end = blockquoteEnd.coerceAtLeast(start))
        }
        blockquoteStart = null
        blockquoteEnd = 0
    }

    private fun flushTable() {
        val start = tableStart ?: return
        val end = tableEnd.coerceAtLeast(start)
        if (tableLineCount >= 2 && tableSeparatorValid) {
            flushText()
            ranges += MarkdownSourceRange(start = start, end = end)
        } else {
            addTextRange(start, end)
        }
        clearTable()
    }

    private fun finishOpenBlocks() {
        if (inCodeFence) ranges += MarkdownSourceRange(start = codeStart, end = markdown.length)
        flushBlockquote()
        flushTable()
        flushText()
    }

    private fun lineEnd(index: Int): Int =
        lineStarts[index] + lines[index].length

    private fun clearTable() {
        tableStart = null
        tableEnd = 0
        tableLineCount = 0
        tableSeparatorValid = false
    }

    private companion object {
        fun buildLineStarts(lines: List<String>): List<Int> {
            var nextStart = 0
            return lines.map { line ->
                val start = nextStart
                nextStart += line.length + 1
                start
            }
        }
    }
}
