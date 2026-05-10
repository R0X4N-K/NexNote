package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

internal fun AnnotatedString.Builder.appendInlineSpans(text: String, colors: MarkdownColors) {
    var index = 0
    while (index < text.length) {
        index = appendInlineToken(text, index, colors)
    }
}

private fun AnnotatedString.Builder.appendInlineToken(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int {
    appendEscapedCharacter(text, index)?.let { return it }
    appendHardBreak(text, index)?.let { return it }
    appendStyledToken(text, index, colors)?.let { return it }
    appendInlineCode(text, index, colors)?.let { return it }
    appendNoteLink(text, index, colors)?.let { return it }
    appendLink(text, index, colors)?.let { return it }
    append(text[index])
    return index + 1
}

private fun AnnotatedString.Builder.appendStyledToken(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int? {
    appendDelimitedStyle(text, index, "~~", strikeStyle, colors)?.let { return it }
    appendDelimitedStyle(text, index, "***", boldItalicStyle, colors)?.let { return it }
    appendDelimitedStyle(text, index, "___", boldItalicStyle, colors)?.let { return it }
    appendDelimitedStyle(text, index, "**", boldStyle, colors)?.let { return it }
    appendDelimitedStyle(text, index, "__", boldStyle, colors)?.let { return it }
    appendSingleAsteriskItalic(text, index, colors)?.let { return it }
    return appendSingleUnderscoreItalic(text, index, colors)
}

private fun AnnotatedString.Builder.appendEscapedCharacter(
    text: String,
    index: Int
): Int? {
    if (text[index] != '\\' || index + 1 >= text.length) return null
    if (!isEscapable(text[index + 1])) return null

    append(text[index + 1])
    return index + 2
}

private fun AnnotatedString.Builder.appendHardBreak(
    text: String,
    index: Int
): Int? =
    when {
        text.startsWith("  \n", index) -> appendBreak(index, 3)
        text.startsWith("\\\n", index) -> appendBreak(index, 2)
        text.startsWith("<br>", index, ignoreCase = true) -> appendBreak(index, 4)
        text.startsWith("<br/>", index, ignoreCase = true) -> appendBreak(index, 5)
        else -> null
    }

private fun AnnotatedString.Builder.appendBreak(index: Int, tagLength: Int): Int {
    append('\n')
    return index + tagLength
}

private fun AnnotatedString.Builder.appendDelimitedStyle(
    text: String,
    index: Int,
    delimiter: String,
    style: SpanStyle,
    colors: MarkdownColors
): Int? {
    if (!text.startsWith(delimiter, index)) return null

    val end = text.indexOf(delimiter, index + delimiter.length)
    if (end == -1) return appendLiteralCharacter(text, index)

    withStyle(style) {
        appendInlineSpans(text.substring(index + delimiter.length, end), colors)
    }
    return end + delimiter.length
}

private fun AnnotatedString.Builder.appendSingleAsteriskItalic(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int? {
    if (text[index] != '*') return null

    val end = findSingleStar(text, index + 1)
    return appendItalicOrLiteral(text, index, end, colors)
}

private fun AnnotatedString.Builder.appendSingleUnderscoreItalic(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int? {
    if (text[index] != '_' || text.getOrNull(index + 1) == '_') return null

    val end = findSingleUnderscore(text, index + 1)
    return appendItalicOrLiteral(text, index, end, colors)
}

private fun AnnotatedString.Builder.appendItalicOrLiteral(
    text: String,
    index: Int,
    end: Int,
    colors: MarkdownColors
): Int {
    if (end == -1) return appendLiteralCharacter(text, index)

    withStyle(italicStyle) {
        appendInlineSpans(text.substring(index + 1, end), colors)
    }
    return end + 1
}

/**
 * Builds the inline-code span style from the active theme colors.
 *
 * The previous implementation hard-coded a translucent black background, which
 * disappeared on dark themes because a near-black tint over a near-black
 * surface produces no perceptible contrast. Sourcing the colors from
 * [MarkdownColors] lets the surrounding Composable supply
 * `surfaceContainerHigh` / `onSurfaceVariant` (or their custom-color analogs)
 * so the `code` span is always readable, regardless of theme or note tint.
 */
private fun inlineCodeStyle(colors: MarkdownColors): SpanStyle =
    SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = colors.inlineCodeBackground,
        color = colors.inlineCodeForeground
    )

private fun AnnotatedString.Builder.appendInlineCode(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int? {
    if (text[index] != '`') return null

    val end = text.indexOf('`', index + 1)
    if (end == -1) return appendLiteralCharacter(text, index)

    withStyle(inlineCodeStyle(colors)) {
        append(text.substring(index + 1, end))
    }
    return end + 1
}

private fun AnnotatedString.Builder.appendNoteLink(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int? {
    val link = NoteLinkMarkdown.parseAt(text, index) ?: return null

    withStyle(SpanStyle(color = colors.linkColor, textDecoration = TextDecoration.Underline)) {
        pushStringAnnotation(
            tag = NoteLinkMarkdown.ANNOTATION_TAG,
            annotation = link.noteId.toString()
        )
        append(link.title)
        pop()
    }
    return link.endIndexExclusive
}

private fun AnnotatedString.Builder.appendLink(
    text: String,
    index: Int,
    colors: MarkdownColors
): Int? {
    if (text[index] != '[' || text.getOrNull(index - 1) == '!') return null

    val closeBracket = text.indexOf(']', index + 1)
    if (closeBracket == -1 || text.getOrNull(closeBracket + 1) != '(') {
        return appendLiteralCharacter(text, index)
    }
    return appendLinkIfClosed(text, index, closeBracket, colors)
}

private fun AnnotatedString.Builder.appendLinkIfClosed(
    text: String,
    index: Int,
    closeBracket: Int,
    colors: MarkdownColors
): Int {
    val closeParen = text.indexOf(')', closeBracket + 2)
    if (closeParen == -1) return appendLiteralCharacter(text, index)

    val label = text.substring(index + 1, closeBracket)
    val url = text.substring(closeBracket + 2, closeParen).withoutOptionalMarkdownTitle()
    withStyle(SpanStyle(color = colors.linkColor, textDecoration = TextDecoration.Underline)) {
        pushStringAnnotation(tag = "URL", annotation = url)
        appendInlineSpans(label, colors)
        pop()
    }
    return closeParen + 1
}

private fun AnnotatedString.Builder.appendLiteralCharacter(text: String, index: Int): Int {
    append(text[index])
    return index + 1
}

/**
 * Italic spans are constructed once at module load and reused across every
 * `*…*` / `_…_` match. Compose's text rendering reuses [SpanStyle] instances
 * by reference inside its style cache, so a single instance keeps the cache
 * hot and avoids allocating a new object for every italic run.
 */
private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
private val strikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
private val boldItalicStyle = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)
private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

private fun String.withoutOptionalMarkdownTitle(): String {
    val trimmed = trim()
    val titleStart = trimmed.indexOf(" \"")
    if (titleStart == -1 || !trimmed.endsWith('"')) return trimmed
    return trimmed.substring(0, titleStart).trim()
}

private fun isEscapable(ch: Char): Boolean =
    ch in "\\`*_{}[]()#+-.!|~"

private fun findSingleStar(text: String, from: Int): Int =
    findSingleDelimiter(text, from, '*')

private fun findSingleUnderscore(text: String, from: Int): Int =
    findSingleDelimiter(text, from, '_')

private fun findSingleDelimiter(text: String, from: Int, delimiter: Char): Int {
    for (index in from until text.length) {
        if (text[index].isSingleDelimiter(text, index, delimiter)) {
            return index
        }
    }
    return -1
}

private fun Char.isSingleDelimiter(text: String, index: Int, delimiter: Char): Boolean =
    this == delimiter &&
        text.getOrNull(index - 1) != delimiter &&
        text.getOrNull(index + 1) != delimiter
