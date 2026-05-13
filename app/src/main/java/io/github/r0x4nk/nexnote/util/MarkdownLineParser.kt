package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val NEST_INDENT_WIDTH = 2
private const val RENDERED_NEST_INDENT = "    "
private const val CHECKBOX_UNCHECKED_MARKER = "☐ "
private const val CHECKBOX_CHECKED_MARKER = "☑ "
private const val UNORDERED_LIST_MARKER = "• "

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

internal fun AnnotatedString.Builder.appendMarkdownLine(line: String, colors: MarkdownColors) {
    val heading = headingMarkers.firstOrNull { line.startsWith(it.prefix) }
    when {
        heading != null -> appendHeading(line, heading, colors)
        appendCheckboxLine(line, MarkdownPatterns.CHECKBOX_UNCHECKED, false, colors) -> Unit
        appendCheckboxLine(line, MarkdownPatterns.CHECKBOX_CHECKED, true, colors) -> Unit
        appendBulletLine(line, colors) -> Unit
        appendOrderedListLine(line, colors) -> Unit
        else -> appendInlineSpans(line, colors)
    }
}

private fun AnnotatedString.Builder.appendHeading(
    line: String,
    heading: HeadingMarker,
    colors: MarkdownColors
) {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = heading.fontSize)) {
        appendInlineSpans(line.removePrefix(heading.prefix), colors)
    }
}

private fun AnnotatedString.Builder.appendCheckboxLine(
    line: String,
    pattern: Regex,
    checked: Boolean,
    colors: MarkdownColors
): Boolean {
    val match = pattern.find(line) ?: return false
    appendNestedIndent(match.groupValues[1])
    append(if (checked) CHECKBOX_CHECKED_MARKER else CHECKBOX_UNCHECKED_MARKER)
    appendCheckboxContent(line.substring(match.value.length), checked, colors)
    return true
}

private fun AnnotatedString.Builder.appendCheckboxContent(
    content: String,
    checked: Boolean,
    colors: MarkdownColors
) {
    if (checked) {
        withStyle(
            SpanStyle(
                color = Color.Gray,
                textDecoration = TextDecoration.LineThrough
            )
        ) {
            appendInlineSpans(content, colors)
        }
    } else {
        appendInlineSpans(content, colors)
    }
}

private fun AnnotatedString.Builder.appendBulletLine(
    line: String,
    colors: MarkdownColors
): Boolean {
    val match = MarkdownPatterns.BULLET_LINE.find(line) ?: return false
    appendNestedIndent(match.groupValues[1])
    append(UNORDERED_LIST_MARKER)
    appendInlineSpans(line.substring(match.value.length), colors)
    return true
}

private fun AnnotatedString.Builder.appendOrderedListLine(
    line: String,
    colors: MarkdownColors
): Boolean {
    val match = MarkdownPatterns.ORDERED_LIST.find(line) ?: return false
    val number = match.groupValues[2]
    appendNestedIndent(match.groupValues[1])
    append("$number. ")
    appendInlineSpans(line.substring(match.value.length), colors)
    return true
}

private fun AnnotatedString.Builder.appendNestedIndent(indent: String) {
    repeat(indent.length / NEST_INDENT_WIDTH) {
        append(RENDERED_NEST_INDENT)
    }
}
