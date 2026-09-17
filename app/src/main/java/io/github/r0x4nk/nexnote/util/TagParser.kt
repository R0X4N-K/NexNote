package io.github.r0x4nk.nexnote.util

/**
 * Pure Kotlin utility for extracting hashtag-style tags from Markdown content.
 *
 * Role: utility layer — no Android or Compose dependencies; fully unit-testable.
 *
 * Tag rules:
 * - A tag starts with '#' that is immediately preceded by whitespace, an inline
 *   code delimiter, or the start of a line (MULTILINE mode).
 * - The tag name starts with a letter [a-zA-Z] and continues with word
 *   characters [a-zA-Z0-9_]. This prevents numeric-only tags like `#123` and
 *   keeps tag names valid identifiers.
 * - Tags are case-insensitive: `#Hello` and `#hello` normalise to "hello".
 * - Duplicate tags within the same note are deduplicated (a [Set] is returned).
 *
 * Deliberate design decisions:
 * - Tags inside fenced code blocks (``` ... ```) are EXCLUDED. The user is
 *   likely writing code, not creating a tag; preserving line count avoids
 *   position drift in error messages.
 * - Tags inside inline code (`` `code` ``) are INCLUDED for simplicity. Users
 *   often write `#tagName` in inline code to reference a tag concept.
 * - `##heading` is NOT a tag: the second '#' is preceded by '#', which is
 *   a non-whitespace character, so the lookbehind rejects it.
 */
object TagParser {

    /**
     * Regex for a hashtag: '#' preceded by whitespace, an inline code delimiter,
     * or start of line, followed by a letter then any word characters.
     * MULTILINE makes '^' match line starts.
     *
     * Group 1 captures the tag name (without '#').
     */
    private val TAG_PATTERN = Regex(
        pattern = """(?:^|[\s`])#([a-zA-Z][a-zA-Z0-9_]*)""",
        options  = setOf(RegexOption.MULTILINE)
    )

    /**
     * Extracts all unique, normalised (lowercase) tag names from [content].
     *
     * Fenced code blocks are stripped before parsing to prevent tags in code
     * examples from being indexed as real note tags.
     *
     * @param content Raw Markdown text from a note.
     * @return A set of lowercase tag names, without the leading '#'.
     */
    fun extractTags(content: String): Set<String> {
        if ('#' !in content) return emptySet()
        val sanitized = if ("```" in content) stripFencedCodeBlocks(content) else content
        // Keep the matcher alive across matches to avoid resetting large input
        // for every hashtag on Android.
        val matcher = TAG_PATTERN.toPattern().matcher(sanitized)
        val tags = linkedSetOf<String>()
        while (matcher.find()) {
            tags.add(matcher.group(1)!!.lowercase())
        }
        return tags
    }

    /**
     * Removes the '#' prefix from every hashtag in [content] while preserving the
     * tag word itself, mirroring the global tag-deletion behaviour.
     *
     * Only real tags are touched: tags inside fenced code blocks are ignored, so
     * code samples stay intact. Heading markers such as `# Title` are not matched
     * because the pattern requires the '#' to be immediately followed by a letter.
     *
     * @param content Raw Markdown text from a note.
     * @return The content with every indexed tag marker stripped.
     */
    fun stripAllTagMarkers(content: String): String {
        if ('#' !in content) return content

        var inFence = false
        return content.lineSequence().joinToString("\n") { line ->
            when {
                line.isFenceDelimiter() -> {
                    inFence = !inFence
                    line
                }
                inFence -> line
                else -> TAG_PATTERN.replace(line) { match ->
                    match.value.replaceFirst("#", "")
                }
            }
        }
    }

    /**
     * Replaces fenced code block lines with newlines to preserve line numbering.
     * The scan is linear and does not skip large notes, so tags inside fences
     * are ignored consistently regardless of content size.
     */
    private fun stripFencedCodeBlocks(content: String): String {
        val sanitized = StringBuilder(content.length)
        var index = 0
        var inFence = false

        while (index < content.length) {
            val lineEnd = content.indexOf('\n', startIndex = index)
                .takeIf { it >= 0 }
                ?: content.length
            val hasNewline = lineEnd < content.length
            val line = content.substring(index, lineEnd)

            if (line.isFenceDelimiter()) {
                inFence = !inFence
                if (hasNewline) sanitized.append('\n')
            } else if (inFence) {
                if (hasNewline) sanitized.append('\n')
            } else {
                sanitized.append(content, index, lineEnd)
                if (hasNewline) sanitized.append('\n')
            }

            index = if (hasNewline) lineEnd + 1 else lineEnd
        }

        return sanitized.toString()
    }

    private fun String.isFenceDelimiter(): Boolean =
        trimStart().startsWith("```")
}
