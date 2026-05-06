package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color

/**
 * Converts Markdown into readable text for clipboard/export surfaces that should
 * not expose Markdown delimiters.
 */
object MarkdownPlainText {

    fun fromMarkdown(markdown: String): String {
        if (markdown.isEmpty()) return ""

        return MarkdownParser.parseBlocks(markdown, Color.Unspecified)
            .mapNotNull { block -> block.toPlainText().takeIf { it.isNotEmpty() } }
            .joinToString("\n")
    }

    private fun MarkdownBlock.toPlainText(): String =
        when (this) {
            is MarkdownBlock.TextBlock -> annotatedString.text
            is MarkdownBlock.BlockquoteBlock -> content.text
            is MarkdownBlock.CodeBlock -> code
            is MarkdownBlock.ImageBlock -> altText
            is MarkdownBlock.TableBlock -> tableToPlainText()
            MarkdownBlock.HorizontalRuleBlock -> ""
        }

    private fun MarkdownBlock.TableBlock.tableToPlainText(): String {
        val lines = buildList {
            add(headers.joinToString("\t") { it.text })
            rows.forEach { row ->
                add(row.joinToString("\t") { it.text })
            }
        }
        return lines.joinToString("\n")
    }
}
