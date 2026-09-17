package io.github.r0x4nk.nexnote.util

private val markdownTaskListLine = Regex("""^(?:>\s?)?\s*[-*+]\s+\[([ xX])]""")

private val markdownUncheckedTaskListLine = Regex(
    pattern = """^(?:>\s?)?\s*[-*+]\s+\[ ]""",
    option = RegexOption.MULTILINE
)

private val markdownCheckedTaskListLine = Regex(
    pattern = """^(?:>\s?)?\s*[-*+]\s+\[[xX]]""",
    option = RegexOption.MULTILINE
)

/**
 * Walks [markdown] line by line, invoking [predicate] on every line that is
 * outside a fenced code block. Iteration stops as soon as [predicate] returns
 * `true`, so bulk checks stay cheap on large notes.
 *
 * Checklist markers inside fenced code blocks are code samples, not real tasks:
 * they are neither rendered as checkboxes nor rewritten by [setAllMarkdownTaskListItems].
 */
private inline fun String.anyLineOutsideFences(predicate: (String) -> Boolean): Boolean {
    var inFence = false
    for (line in lineSequence()) {
        if (line.isFenceDelimiter()) {
            inFence = !inFence
            continue
        }
        if (!inFence && predicate(line)) return true
    }
    return false
}

private fun String.isFenceDelimiter(): Boolean = trimStart().startsWith("```")

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

/**
 * Whether the note contains at least one unchecked task-list item outside a
 * fenced code block. Used to decide whether offering "check all" is meaningful.
 */
internal fun hasUncheckedMarkdownTaskListItems(markdown: String): Boolean =
    markdown.anyLineOutsideFences { markdownUncheckedTaskListLine.containsMatchIn(it) }

/**
 * Whether the note contains at least one checked task-list item outside a
 * fenced code block.
 *
 * See [hasUncheckedMarkdownTaskListItems] for the fence rationale.
 */
internal fun hasCheckedMarkdownTaskListItems(markdown: String): Boolean =
    markdown.anyLineOutsideFences { markdownCheckedTaskListLine.containsMatchIn(it) }

/**
 * Sets every task-list marker in [markdown] to checked (`[x]`) or unchecked
 * (`[ ]`) in one pass.
 *
 * Indentation and blockquote prefixes are preserved because only the marker
 * character captured by the regex is replaced. Non-task lines and fenced code
 * blocks are left untouched, so this can safely rewrite an entire note.
 */
internal fun setAllMarkdownTaskListItems(markdown: String, checked: Boolean): String {
    if ("[" !in markdown) return markdown
    val replacement = if (checked) "x" else " "

    var inFence = false
    return markdown.lineSequence().joinToString("\n") { line ->
        if (line.isFenceDelimiter()) {
            inFence = !inFence
            line
        } else if (inFence) {
            line
        } else {
            markdownTaskListLine.replace(line) { match ->
                val markerRange = match.groups[1]!!.range
                val relativeStart = markerRange.first - match.range.first
                match.value.replaceRange(relativeStart, relativeStart + 1, replacement)
            }
        }
    }
}
