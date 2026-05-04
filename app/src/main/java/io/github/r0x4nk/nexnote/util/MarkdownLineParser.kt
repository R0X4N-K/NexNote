package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val NEST_INDENT_WIDTH = 2
private const val RENDERED_NEST_INDENT = "    "
private const val CHECKBOX_UNCHECKED_MARKER = "\u2610 "
private const val CHECKBOX_CHECKED_MARKER = "\u2611 "
private const val UNORDERED_LIST_MARKER = "\u2022 "

private data class HeadingMarker(
    val prefix: String,
    val fontSize: TextUnit
)

private val headingMarkers = listOf(
    HeadingMarker("###### ", 12.sp),
    HeadingMarker("##### ", 13.sp),
    HeadingMarker("#### ", 15.sp),
    HeadingMarker("### ", 18.sp),
    HeadingMarker("## ", 22.sp),
    HeadingMarker("# ", 28.sp)
)

internal fun AnnotatedString.Builder.appendMarkdownLine(line: String, linkColor: Color) {
    val heading = headingMarkers.firstOrNull { line.startsWith(it.prefix) }
    when {
        heading != null -> appendHeading(line, heading, linkColor)
        appendCheckboxLine(line, MarkdownPatterns.CHECKBOX_UNCHECKED, false, linkColor) -> Unit
        appendCheckboxLine(line, MarkdownPatterns.CHECKBOX_CHECKED, true, linkColor) -> Unit
        appendBulletLine(line, linkColor) -> Unit
        appendOrderedListLine(line, linkColor) -> Unit
        else -> appendInlineSpans(line, linkColor)
    }
}

private fun AnnotatedString.Builder.appendHeading(
    line: String,
    heading: HeadingMarker,
    linkColor: Color
) {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = heading.fontSize)) {
        appendInlineSpans(line.removePrefix(heading.prefix), linkColor)
    }
}

private fun AnnotatedString.Builder.appendCheckboxLine(
    line: String,
    pattern: Regex,
    checked: Boolean,
    linkColor: Color
): Boolean {
    val match = pattern.find(line) ?: return false
    appendNestedIndent(match.groupValues[1])
    append(if (checked) CHECKBOX_CHECKED_MARKER else CHECKBOX_UNCHECKED_MARKER)
    appendCheckboxContent(line.substring(match.value.length), checked, linkColor)
    return true
}

private fun AnnotatedString.Builder.appendCheckboxContent(
    content: String,
    checked: Boolean,
    linkColor: Color
) {
    if (checked) {
        withStyle(SpanStyle(color = Color.Gray)) {
            appendInlineSpans(content, linkColor)
        }
    } else {
        appendInlineSpans(content, linkColor)
    }
}

private fun AnnotatedString.Builder.appendBulletLine(
    line: String,
    linkColor: Color
): Boolean {
    val match = MarkdownPatterns.BULLET_LINE.find(line) ?: return false
    appendNestedIndent(match.groupValues[1])
    append(UNORDERED_LIST_MARKER)
    appendInlineSpans(line.substring(match.value.length), linkColor)
    return true
}

private fun AnnotatedString.Builder.appendOrderedListLine(
    line: String,
    linkColor: Color
): Boolean {
    val match = MarkdownPatterns.ORDERED_LIST.find(line) ?: return false
    val number = match.groupValues[2]
    appendNestedIndent(match.groupValues[1])
    append("$number. ")
    appendInlineSpans(line.substring(match.value.length), linkColor)
    return true
}

private fun AnnotatedString.Builder.appendNestedIndent(indent: String) {
    repeat(indent.length / NEST_INDENT_WIDTH) {
        append(RENDERED_NEST_INDENT)
    }
}
