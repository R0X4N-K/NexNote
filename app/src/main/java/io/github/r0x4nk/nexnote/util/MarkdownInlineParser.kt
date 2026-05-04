package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

internal fun AnnotatedString.Builder.appendInlineSpans(text: String, linkColor: Color) {
    var index = 0
    while (index < text.length) {
        index = appendInlineToken(text, index, linkColor)
    }
}

private fun AnnotatedString.Builder.appendInlineToken(
    text: String,
    index: Int,
    linkColor: Color
): Int {
    appendEscapedCharacter(text, index)?.let { return it }
    appendHardBreak(text, index)?.let { return it }
    appendStyledToken(text, index, linkColor)?.let { return it }
    appendInlineCode(text, index)?.let { return it }
    appendNoteLink(text, index, linkColor)?.let { return it }
    appendLink(text, index, linkColor)?.let { return it }
    append(text[index])
    return index + 1
}

private fun AnnotatedString.Builder.appendStyledToken(
    text: String,
    index: Int,
    linkColor: Color
): Int? {
    appendDelimitedStyle(text, index, "~~", strikeStyle, linkColor)?.let { return it }
    appendDelimitedStyle(text, index, "***", boldItalicStyle, linkColor)?.let { return it }
    appendDelimitedStyle(text, index, "**", boldStyle, linkColor)?.let { return it }
    appendDelimitedStyle(text, index, "__", boldStyle, linkColor)?.let { return it }
    appendSingleAsteriskItalic(text, index, linkColor)?.let { return it }
    return appendSingleUnderscoreItalic(text, index, linkColor)
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
    linkColor: Color
): Int? {
    if (!text.startsWith(delimiter, index)) return null

    val end = text.indexOf(delimiter, index + delimiter.length)
    if (end == -1) return appendLiteralCharacter(text, index)

    withStyle(style) {
        appendInlineSpans(text.substring(index + delimiter.length, end), linkColor)
    }
    return end + delimiter.length
}

private fun AnnotatedString.Builder.appendSingleAsteriskItalic(
    text: String,
    index: Int,
    linkColor: Color
): Int? {
    if (text[index] != '*') return null

    val end = findSingleStar(text, index + 1)
    return appendItalicOrLiteral(text, index, end, linkColor)
}

private fun AnnotatedString.Builder.appendSingleUnderscoreItalic(
    text: String,
    index: Int,
    linkColor: Color
): Int? {
    if (text[index] != '_' || text.getOrNull(index + 1) == '_') return null

    val end = findSingleUnderscore(text, index + 1)
    return appendItalicOrLiteral(text, index, end, linkColor)
}

private fun AnnotatedString.Builder.appendItalicOrLiteral(
    text: String,
    index: Int,
    end: Int,
    linkColor: Color
): Int {
    if (end == -1) return appendLiteralCharacter(text, index)

    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
        appendInlineSpans(text.substring(index + 1, end), linkColor)
    }
    return end + 1
}

private fun AnnotatedString.Builder.appendInlineCode(
    text: String,
    index: Int
): Int? {
    if (text[index] != '`') return null

    val end = text.indexOf('`', index + 1)
    if (end == -1) return appendLiteralCharacter(text, index)

    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x14000000))) {
        append(text.substring(index + 1, end))
    }
    return end + 1
}

private fun AnnotatedString.Builder.appendNoteLink(
    text: String,
    index: Int,
    linkColor: Color
): Int? {
    val link = NoteLinkMarkdown.parseAt(text, index) ?: return null

    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
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
    linkColor: Color
): Int? {
    if (text[index] != '[' || text.getOrNull(index - 1) == '!') return null

    val closeBracket = text.indexOf(']', index + 1)
    if (closeBracket == -1 || text.getOrNull(closeBracket + 1) != '(') {
        return appendLiteralCharacter(text, index)
    }
    return appendLinkIfClosed(text, index, closeBracket, linkColor)
}

private fun AnnotatedString.Builder.appendLinkIfClosed(
    text: String,
    index: Int,
    closeBracket: Int,
    linkColor: Color
): Int {
    val closeParen = text.indexOf(')', closeBracket + 2)
    if (closeParen == -1) return appendLiteralCharacter(text, index)

    val label = text.substring(index + 1, closeBracket)
    val url = text.substring(closeBracket + 2, closeParen)
    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
        pushStringAnnotation(tag = "URL", annotation = url)
        appendInlineSpans(label, linkColor)
        pop()
    }
    return closeParen + 1
}

private fun AnnotatedString.Builder.appendLiteralCharacter(text: String, index: Int): Int {
    append(text[index])
    return index + 1
}

private val strikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
private val boldItalicStyle = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)
private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

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
