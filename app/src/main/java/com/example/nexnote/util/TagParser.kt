package com.example.nexnote.util

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
     * Regex for fenced code blocks: triple backtick followed by an optional
     * language hint, a newline, any content, and a closing triple backtick.
     * Uses non-greedy matching to avoid consuming multiple code blocks as one.
     */
    private val FENCED_CODE_BLOCK = Regex("""```[a-zA-Z]*\n[\s\S]*?```""")

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
        val sanitized = stripFencedCodeBlocks(content)
        return TAG_PATTERN.findAll(sanitized)
            .map { match -> match.groupValues[1].lowercase() }
            .toSet()
    }

    /**
     * Replaces fenced code block content with an equal number of newlines to
     * preserve line numbering. The surrounding ``` markers are also replaced so
     * that `#language` hints inside the opening fence are not extracted as tags.
     */
    private fun stripFencedCodeBlocks(content: String): String {
        if (content.length > 500_000) return content
        return FENCED_CODE_BLOCK.replace(content) { match ->
            "\n".repeat(match.value.count { it == '\n' })
        }
    }
}
