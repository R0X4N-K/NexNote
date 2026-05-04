package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.AnnotatedString

/** Horizontal alignment of a table column, as specified by its separator row. */
enum class ColumnAlignment { LEFT, CENTER, RIGHT }

/**
 * Represents a single display block produced by [MarkdownParser.parseBlocks].
 * Text and images are kept separate so the UI can render them differently.
 */
sealed class MarkdownBlock {
    /** A run of styled inline text (may contain bold, italic, links, code). */
    data class TextBlock(val annotatedString: AnnotatedString) : MarkdownBlock()

    /** An embedded image referenced by a markdown `![altText](path)` tag. */
    data class ImageBlock(val path: String, val altText: String) : MarkdownBlock()

    /** A horizontal rule (`---`, `***`, `___`, etc.). */
    data object HorizontalRuleBlock : MarkdownBlock()

    /** A blockquote (`> text`). The content is inline-parsed markdown. */
    data class BlockquoteBlock(val content: AnnotatedString) : MarkdownBlock()

    /**
     * A fenced code block (triple backticks or tildes).
     * Content is raw text; no inline markdown processing is applied.
     */
    data class CodeBlock(val code: String, val language: String?) : MarkdownBlock()

    /**
     * A GFM-style pipe table.
     *
     * [headers] and each row in [rows] are already inline-parsed so they may
     * contain bold, italic, links, etc. [alignments] has exactly [headers].size
     * entries; missing separator cells default to [ColumnAlignment.LEFT].
     */
    data class TableBlock(
        val headers: List<AnnotatedString>,
        val alignments: List<ColumnAlignment>,
        val rows: List<List<AnnotatedString>>
    ) : MarkdownBlock()
}
