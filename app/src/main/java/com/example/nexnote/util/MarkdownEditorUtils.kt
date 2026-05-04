package com.example.nexnote.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Result of inserting text into a Markdown document.
 *
 * [cursorOffset] points to the best position for continued typing after the
 * inserted block.
 */
data class MarkdownInsertionResult(
    val text: String,
    val cursorOffset: Int
)

/**
 * Intercepts a [TextFieldValue] change and, when the user pressed Enter at the
 * end of a list item, automatically inserts the next list marker so the list
 * continues without requiring manual re-typing of the prefix.
 *
 * ### Supported continuations
 * - `- item`    → next line starts with `- `
 * - `* item`    → next line starts with `* `
 * - `1. item`   → next line starts with `2. `
 * - `- [ ] item`→ next line starts with `- [ ] `
 * - Indented variants of all the above (nesting preserved)
 *
 * ### Stop condition
 * If the line that was completed had no content after the list marker (i.e. the
 * user pressed Enter on an empty `- ` line), the function returns `null` so the
 * caller can simply accept the unmodified [newValue].  This mirrors the behaviour
 * of most Markdown editors: an empty bullet ends the list.
 *
 * @param oldValue The [TextFieldValue] before the keystroke.
 * @param newValue The [TextFieldValue] produced by [BasicTextField] after the keystroke.
 * @return A modified [TextFieldValue] with the list marker inserted, or [newValue]
 *         unchanged if no continuation applies.
 */
fun handleSmartEnter(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    // Only act when exactly one character was inserted (a normal Enter keystroke).
    // Pastes, deletions, or multi-character replacements are left untouched.
    if (new.text.length != old.text.length + 1) return new

    val cursorPos  = new.selection.start
    val insertedAt = cursorPos - 1
    if (insertedAt < 0 || new.text.getOrNull(insertedAt) != '\n') return new

    // The line that the user just completed sits between the previous newline
    // (or the start of the document) and the character that was just inserted.
    val textBeforeCursor = new.text.substring(0, insertedAt)
    val prevLineStart    = textBeforeCursor.lastIndexOf('\n') + 1  // 0 if no prior newline
    val prevLine         = textBeforeCursor.substring(prevLineStart)

    val insertion = computeListContinuation(prevLine) ?: return new

    // Splice the marker right after the newly inserted newline.
    val newText = new.text.substring(0, cursorPos) + insertion + new.text.substring(cursorPos)
    return TextFieldValue(
        text      = newText,
        selection = TextRange(cursorPos + insertion.length)
    )
}

/**
 * Inserts [block] as its own Markdown line at [offset].
 *
 * The function keeps images and other block-level snippets parseable by ensuring
 * there is a line break before and after the block when neighbouring text exists.
 */
fun insertStandaloneMarkdownBlock(
    text: String,
    block: String,
    offset: Int
): MarkdownInsertionResult {
    val safeOffset = offset.coerceIn(0, text.length)
    val before     = text.substring(0, safeOffset)
    val after      = text.substring(safeOffset)
    val prefix     = if (before.isNotEmpty() && !before.endsWith('\n')) "\n" else ""
    val suffix     = if (after.startsWith('\n')) "" else "\n"
    val newText    = before + prefix + block + suffix + after

    val existingTrailingBreakLength = if (suffix.isEmpty() && after.startsWith('\n')) 1 else 0
    val cursorOffset = before.length +
        prefix.length +
        block.length +
        suffix.length +
        existingTrailingBreakLength

    return MarkdownInsertionResult(
        text         = newText,
        cursorOffset = cursorOffset.coerceIn(0, newText.length)
    )
}

/**
 * Determines what text to auto-insert after a newline that follows [line].
 *
 * Returns the continuation string (e.g. `"- "`, `"2. "`, `"  - [ ] "`), or
 * `null` if [line] is not a list item.
 */
private fun computeListContinuation(line: String): String? {
    if (line.isBlank()) return null

    val trimmed = line.trimStart()
    val indent  = " ".repeat(line.length - trimmed.length)

    // Task list must be checked before the plain bullet so that `- [ ] text`
    // is not consumed by the simpler bullet pattern.
    val taskMatch = Regex("""^[*\-]\s+\[[ xX]]\s*(.*)""").find(trimmed)
    if (taskMatch != null) return "$indent- [ ] "

    val orderedMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
    if (orderedMatch != null) {
        val nextNumber = (orderedMatch.groupValues[1].toIntOrNull() ?: 1) + 1
        return "$indent$nextNumber. "
    }

    val bulletMatch = Regex("""^([*\-])\s+(.*)""").find(trimmed)
    if (bulletMatch != null) return "$indent${bulletMatch.groupValues[1]} "

    return null
}
