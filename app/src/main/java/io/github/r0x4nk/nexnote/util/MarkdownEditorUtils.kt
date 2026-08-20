package io.github.r0x4nk.nexnote.util

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
internal fun markdownListContinuationForLine(line: String): String? {
    if (line.isBlank()) return null

    val trimmed = line.trimStart()
    val indent  = " ".repeat(line.length - trimmed.length)

    // Task list must be checked before the plain bullet so that `- [ ] text`
    // is not consumed by the simpler bullet pattern.
    val taskMatch = Regex("""^[*\-+]\s+\[[ xX]]\s*(.*)""").find(trimmed)
    if (taskMatch != null) return "$indent- [ ] "

    val quoteMatch = Regex("""^>\s+(.*)""").find(trimmed)
    if (quoteMatch != null && quoteMatch.groupValues[1].isNotBlank()) return "$indent> "

    val orderedMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
    if (orderedMatch != null) {
        val nextNumber = (orderedMatch.groupValues[1].toIntOrNull() ?: 1) + 1
        return "$indent$nextNumber. "
    }

    val bulletMatch = Regex("""^([*\-+])\s+(.*)""").find(trimmed)
    if (bulletMatch != null) return "$indent${bulletMatch.groupValues[1]} "

    return null
}
