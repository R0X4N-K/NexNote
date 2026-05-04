package com.example.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

internal fun parseTableRow(line: String, linkColor: Color): List<AnnotatedString> =
    line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { cell ->
            buildAnnotatedString { appendInlineSpans(cell.trim(), linkColor) }
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
