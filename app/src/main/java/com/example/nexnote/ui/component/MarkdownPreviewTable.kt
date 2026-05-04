package com.example.nexnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexnote.util.ColumnAlignment
import com.example.nexnote.util.MarkdownBlock

/**
 * Renders a [MarkdownBlock.TableBlock] as a bordered table with a highlighted
 * header row. Columns share equal width via [Modifier.weight]; a horizontal
 * scroll wraps the whole table so wide content is accessible.
 */
@Composable
internal fun MarkdownTableBlock(
    table: MarkdownBlock.TableBlock,
    style: TextStyle,
    onNoteLinkClick: (Long) -> Unit
) {
    val columnCount = table.headers.size
    if (columnCount == 0) return

    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        MarkdownTableHeader(table, style, headerBg, uriHandler, onNoteLinkClick)
        HorizontalDivider(thickness = 1.5.dp, color = borderColor)
        MarkdownTableRows(table, style, borderColor, uriHandler, onNoteLinkClick)
    }
}

@Composable
private fun MarkdownTableHeader(
    table: MarkdownBlock.TableBlock,
    style: TextStyle,
    headerBg: androidx.compose.ui.graphics.Color,
    uriHandler: UriHandler,
    onNoteLinkClick: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
    ) {
        table.headers.forEachIndexed { col, headerCell ->
            val alignment = table.alignments.getOrElse(col) { ColumnAlignment.LEFT }
            TableCell(
                content    = headerCell,
                style      = style.copy(fontWeight = FontWeight.Bold),
                alignment  = alignment,
                uriHandler = uriHandler,
                onNoteLinkClick = onNoteLinkClick,
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MarkdownTableRows(
    table: MarkdownBlock.TableBlock,
    style: TextStyle,
    borderColor: androidx.compose.ui.graphics.Color,
    uriHandler: UriHandler,
    onNoteLinkClick: (Long) -> Unit
) {
    val columnCount = table.headers.size
    table.rows.forEachIndexed { rowIndex, row ->
        if (rowIndex > 0) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color     = borderColor.copy(alpha = 0.5f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(columnCount) { col ->
                val cell = row.getOrElse(col) { AnnotatedString("") }
                val alignment = table.alignments.getOrElse(col) { ColumnAlignment.LEFT }
                TableCell(
                    content    = cell,
                    style      = style,
                    alignment  = alignment,
                    uriHandler = uriHandler,
                    onNoteLinkClick = onNoteLinkClick,
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TableCell(
    content: AnnotatedString,
    style: TextStyle,
    alignment: ColumnAlignment,
    uriHandler: UriHandler,
    onNoteLinkClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        modifier         = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = alignment.toContentAlignment()
    ) {
        BasicText(
            text         = content,
            style        = style.copy(color = MaterialTheme.colorScheme.onSurface),
            onTextLayout = { layoutResult = it },
            modifier     = Modifier
                .fillMaxWidth()
                .markdownAnnotationTapHandler(
                    displayText = content,
                    getLayoutResult = { layoutResult },
                    openUri = uriHandler::openUri,
                    onNoteLinkClick = onNoteLinkClick
                )
        )
    }
}

private fun ColumnAlignment.toContentAlignment(): Alignment =
    when (this) {
        ColumnAlignment.CENTER -> Alignment.Center
        ColumnAlignment.RIGHT  -> Alignment.CenterEnd
        ColumnAlignment.LEFT   -> Alignment.CenterStart
    }
