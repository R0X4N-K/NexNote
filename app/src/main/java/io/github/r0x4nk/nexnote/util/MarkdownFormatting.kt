package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.TextRange

/**
 * Text plus the selection that should be applied after a Markdown edit.
 */
data class MarkdownTextEdit(
    val text: String,
    val selection: TextRange
)

object MarkdownInlineToggle {
    fun bold(text: String, selection: TextRange): MarkdownTextEdit =
        toggleDelimited(text, selection.normalized(), "**", "**", placeholder = "text")

    fun italic(text: String, selection: TextRange): MarkdownTextEdit =
        toggleDelimited(text, selection.normalized(), "*", "*", placeholder = "text")

    fun inlineCode(text: String, selection: TextRange): MarkdownTextEdit =
        toggleDelimited(text, selection.normalized(), "`", "`", placeholder = "code")

    fun strikethrough(text: String, selection: TextRange): MarkdownTextEdit =
        toggleDelimited(text, selection.normalized(), "~~", "~~", placeholder = "text")

    fun highlight(text: String, selection: TextRange): MarkdownTextEdit =
        toggleDelimited(text, selection.normalized(), "==", "==", placeholder = "text")

    fun link(text: String, selection: TextRange): MarkdownTextEdit {
        val safeSelection = selection.normalized().coerceIn(text.length)
        val selectedText = text.substring(safeSelection.start, safeSelection.end)
        val label = selectedText.ifEmpty { "text" }
        val insertion = "[$label](url)"
        val nextText = text.replaceRange(safeSelection.start, safeSelection.end, insertion)
        val selectionStart = if (selectedText.isEmpty()) {
            safeSelection.start + 1
        } else {
            safeSelection.start + insertion.length - "url)".length
        }
        val selectionEnd = if (selectedText.isEmpty()) {
            selectionStart + label.length
        } else {
            selectionStart + "url".length
        }
        return MarkdownTextEdit(nextText, TextRange(selectionStart, selectionEnd))
    }

    private fun toggleDelimited(
        text: String,
        selection: TextRange,
        opening: String,
        closing: String,
        placeholder: String = ""
    ): MarkdownTextEdit {
        val safeSelection = selection.coerceIn(text.length)
        if (safeSelection.collapsed) {
            // Insert a selected placeholder so the preview immediately renders
            // the formatted span and the user can overtype to replace it. Without
            // a placeholder the inserted markers may collide with each other —
            // for example italic's "**" looks like an unclosed bold delimiter
            // and renders as nothing in the preview.
            val insertion = opening + placeholder + closing
            val nextText = text.replaceRange(safeSelection.start, safeSelection.end, insertion)
            val selectionStart = safeSelection.start + opening.length
            val selectionEnd = selectionStart + placeholder.length
            return MarkdownTextEdit(nextText, TextRange(selectionStart, selectionEnd))
        }

        val start = safeSelection.start
        val end = safeSelection.end
        val hasOuterDelimiters = text.hasRange(start - opening.length, start, opening) &&
            text.hasRange(end, end + closing.length, closing)
        if (hasOuterDelimiters) {
            val nextText = buildString {
                append(text.substring(0, start - opening.length))
                append(text.substring(start, end))
                append(text.substring(end + closing.length))
            }
            return MarkdownTextEdit(nextText, TextRange(start - opening.length, end - opening.length))
        }

        val selectedText = text.substring(start, end)
        if (selectedText.startsWith(opening) && selectedText.endsWith(closing)) {
            val nextSelectedText = selectedText.removePrefix(opening).removeSuffix(closing)
            val nextText = text.replaceRange(start, end, nextSelectedText)
            return MarkdownTextEdit(nextText, TextRange(start, start + nextSelectedText.length))
        }

        val nextSelectedText = opening + selectedText + closing
        val nextText = text.replaceRange(start, end, nextSelectedText)
        return MarkdownTextEdit(nextText, TextRange(start + opening.length, end + opening.length))
    }
}

object MarkdownLineToggle {
    /**
     * Cycles the heading level on every selected line:
     * plain → H1 → H2 → H3 → plain. Useful as a quick toolbar toggle.
     */
    fun heading(text: String, selection: TextRange): MarkdownTextEdit =
        editSelectedLines(text, selection) { lines ->
            lines.map { line ->
                val match = HEADING.matchEntire(line)
                when (match?.groupValues?.get(1)?.length) {
                    null -> "# $line"
                    1 -> "## ${match.groupValues[2]}"
                    2 -> "### ${match.groupValues[2]}"
                    else -> match.groupValues[2]
                }
            }
        }

    /**
     * Forces every selected line to the requested heading [level] (1..6).
     * If the lines are already at that level the prefix is removed instead so
     * the user can toggle the chosen heading off without having to cycle
     * through every other level. Empty lines are still tagged with the prefix
     * so the user can immediately start typing the heading title.
     */
    fun setHeading(text: String, selection: TextRange, level: Int): MarkdownTextEdit {
        val safeLevel = level.coerceIn(1, 6)
        val prefix = "#".repeat(safeLevel) + " "
        return editSelectedLines(text, selection) { lines ->
            val nonBlank = lines.filter { it.isNotBlank() }
            val targetRegex = Regex("""^#{$safeLevel}\s+""")
            val allMatchTarget = nonBlank.isNotEmpty() && nonBlank.all { targetRegex.containsMatchIn(it) }
            lines.map { line ->
                when {
                    line.isBlank() && !allMatchTarget -> prefix
                    line.isBlank() -> line
                    allMatchTarget -> targetRegex.replace(line, "")
                    else -> prefix + ANY_HEADING.replace(line, "")
                }
            }
        }
    }

    fun quote(text: String, selection: TextRange): MarkdownTextEdit =
        toggleLinePrefix(text, selection, "> ", Regex("""^>\s?"""))

    fun unorderedList(text: String, selection: TextRange): MarkdownTextEdit =
        toggleLinePrefix(text, selection, "- ", Regex("""^\s*[*+\-]\s+"""))

    fun orderedList(text: String, selection: TextRange): MarkdownTextEdit =
        editSelectedLines(text, selection) { lines ->
            val nonBlank = lines.filter { it.isNotBlank() }
            val allOrdered = nonBlank.isNotEmpty() && nonBlank.all { ORDERED_LIST.containsMatchIn(it) }
            // When the cursor is on a single empty line the user simply wants
            // a numbered marker inserted so they can start typing the first
            // item — without this the click would silently do nothing.
            if (lines.size == 1 && lines.single().isBlank()) {
                return@editSelectedLines listOf("1. ")
            }
            lines.mapIndexed { index, line ->
                when {
                    allOrdered -> ORDERED_LIST.replace(line, "")
                    line.isBlank() -> line
                    else -> "${index + 1}. $line"
                }
            }
        }


    fun codeBlock(text: String, selection: TextRange): MarkdownTextEdit {
        val safeSelection = selection.normalized().coerceIn(text.length)
        val selectedText = text.substring(safeSelection.start, safeSelection.end)
        val block = if (selectedText.isEmpty()) {
            "```\n\n```"
        } else {
            "```\n$selectedText\n```"
        }
        val nextText = text.replaceRange(safeSelection.start, safeSelection.end, block)
        val cursor = safeSelection.start + "```\n".length
        val nextSelection = if (selectedText.isEmpty()) {
            TextRange(cursor)
        } else {
            TextRange(cursor, cursor + selectedText.length)
        }
        return MarkdownTextEdit(nextText, nextSelection)
    }

    fun horizontalRule(text: String, selection: TextRange): MarkdownTextEdit {
        val safeSelection = selection.normalized().coerceIn(text.length)
        val prefix = if (safeSelection.start == 0 || text.getOrNull(safeSelection.start - 1) == '\n') "" else "\n"
        val suffix = if (text.getOrNull(safeSelection.start) == '\n') "" else "\n"
        val insertion = "$prefix---$suffix"
        val nextText = text.replaceRange(safeSelection.start, safeSelection.end, insertion)
        val cursor = safeSelection.start + insertion.length
        return MarkdownTextEdit(nextText, TextRange(cursor))
    }

    private fun toggleLinePrefix(
        text: String,
        selection: TextRange,
        prefix: String,
        existingPrefix: Regex
    ): MarkdownTextEdit =
        editSelectedLines(text, selection) { lines ->
            val nonBlankLines = lines.filter { it.isNotBlank() }
            val removePrefix = nonBlankLines.isNotEmpty() && nonBlankLines.all { existingPrefix.containsMatchIn(it) }
            // When the cursor sits on a single empty line (no selection, e.g.
            // the user just pressed Enter and clicked "list"), insert the
            // marker so they can start typing the first item right away.
            if (lines.size == 1 && lines.single().isBlank()) {
                return@editSelectedLines listOf(prefix)
            }
            lines.map { line ->
                when {
                    line.isBlank() -> line
                    removePrefix -> existingPrefix.replace(line, "")
                    else -> prefix + line
                }
            }
        }

    private fun editSelectedLines(
        text: String,
        selection: TextRange,
        transform: (List<String>) -> List<String>
    ): MarkdownTextEdit {
        val safeSelection = selection.normalized().coerceIn(text.length)
        val start = text.lineStartFor(safeSelection.start)
        val effectiveEnd = if (!safeSelection.collapsed && safeSelection.end > 0 &&
            text.getOrNull(safeSelection.end - 1) == '\n'
        ) {
            safeSelection.end - 1
        } else {
            safeSelection.end
        }
        val end = text.lineEndFor(effectiveEnd)
        val original = text.substring(start, end)
        val next = transform(original.split("\n")).joinToString("\n")
        val nextText = text.replaceRange(start, end, next)
        return MarkdownTextEdit(nextText, TextRange(start + next.length))
    }
}

private val HEADING = Regex("""^(#{1,3})\s+(.*)$""")
private val ANY_HEADING = Regex("""^#{1,6}\s+""")
private val ORDERED_LIST = Regex("""^\s*\d+\.\s+""")

private fun TextRange.normalized(): TextRange =
    if (start <= end) this else TextRange(end, start)

private fun TextRange.coerceIn(textLength: Int): TextRange =
    TextRange(start.coerceIn(0, textLength), end.coerceIn(0, textLength))

private fun String.hasRange(start: Int, end: Int, value: String): Boolean =
    start >= 0 && end <= length && substring(start, end) == value

private fun String.lineStartFor(offset: Int): Int =
    if (offset <= 0) {
        0
    } else {
        lastIndexOf('\n', offset - 1).let { if (it == -1) 0 else it + 1 }
    }

private fun String.lineEndFor(offset: Int): Int =
    indexOf('\n', offset.coerceIn(0, length)).let { if (it == -1) length else it }
