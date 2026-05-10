package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

internal fun parseTableRow(line: String, colors: MarkdownColors): List<AnnotatedString> =
    line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { cell ->
            buildAnnotatedString { appendInlineSpans(cell.trim(), colors) }
        }

internal fun parseSeparatorRow(line: String, columnCount: Int): List<ColumnAlignment> {
    val cells = line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }

    return List(columnCount) { index ->
        val cell = cells.getOrElse(index) { "-" }
        when {
            cell.startsWith(":") && cell.endsWith(":") -> ColumnAlignment.CENTER
            cell.endsWith(":") -> ColumnAlignment.RIGHT
            else -> ColumnAlignment.LEFT
        }
    }
}
