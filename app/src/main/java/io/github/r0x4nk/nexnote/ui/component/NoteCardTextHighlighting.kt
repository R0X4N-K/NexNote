package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

internal fun buildNoteCardHighlightedText(
    text: String,
    ranges: List<IntRange>,
    highlightColor: Color
): AnnotatedString {
    if (ranges.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            val safeStart = range.first.coerceIn(0, text.length)
            val safeEnd = (range.last + 1).coerceIn(safeStart, text.length)
            if (safeStart < safeEnd) {
                addStyle(
                    SpanStyle(background = highlightColor.copy(alpha = 0.25f)),
                    start = safeStart,
                    end = safeEnd
                )
            }
        }
    }
}
