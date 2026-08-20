package io.github.r0x4nk.nexnote.util

private val markdownTaskListLine = Regex("""^(?:>\s?)?\s*[-*+]\s+\[([ xX])]""")

/**
 * Resolves the source offset of a rendered task-list marker.
 *
 * [lineIndex] is relative to the rendered Markdown block. Keeping the parser
 * annotation line-based makes the mapping stable when inline formatting hides
 * source delimiters from the preview.
 */
internal fun findMarkdownTaskListMarkerOffset(
    markdown: String,
    sourceStart: Int,
    sourceEnd: Int,
    lineIndex: Int
): Int? {
    if (lineIndex < 0 || sourceStart !in 0..markdown.length) return null
    if (sourceEnd !in sourceStart..markdown.length) return null

    var lineStart = sourceStart
    repeat(lineIndex) {
        val newline = markdown.indexOf('\n', startIndex = lineStart)
        if (newline == -1 || newline >= sourceEnd) return null
        lineStart = newline + 1
    }
    if (lineStart > sourceEnd) return null

    val nextNewline = markdown.indexOf('\n', startIndex = lineStart)
    val lineEnd = if (nextNewline == -1 || nextNewline > sourceEnd) {
        sourceEnd
    } else {
        nextNewline
    }
    val marker = markdownTaskListLine.find(markdown.substring(lineStart, lineEnd))
        ?.groups
        ?.get(1)
        ?: return null
    return lineStart + marker.range.first
}

/**
 * Toggles a validated Markdown task marker at [markerOffset].
 *
 * Unchecked items use `[ ]`; both lowercase and uppercase checked markers are
 * accepted and normalized back to `[ ]` when the item is reopened.
 */
internal fun toggleMarkdownTaskListItem(markdown: String, markerOffset: Int): String? {
    if (markerOffset !in markdown.indices) return null

    val lineStart = if (markerOffset == 0) {
        0
    } else {
        markdown.lastIndexOf('\n', startIndex = markerOffset - 1) + 1
    }
    val nextNewline = markdown.indexOf('\n', startIndex = markerOffset)
    val lineEnd = if (nextNewline == -1) markdown.length else nextNewline
    val marker = markdownTaskListLine.find(markdown.substring(lineStart, lineEnd))
        ?.groups
        ?.get(1)
        ?: return null
    val validatedOffset = lineStart + marker.range.first
    if (validatedOffset != markerOffset) return null

    val replacement = when (markdown[markerOffset]) {
        ' ' -> 'x'
        'x', 'X' -> ' '
        else -> return null
    }
    return markdown.replaceRange(markerOffset, markerOffset + 1, replacement.toString())
}
