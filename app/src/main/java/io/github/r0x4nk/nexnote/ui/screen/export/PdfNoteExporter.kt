package io.github.r0x4nk.nexnote.ui.screen.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.ColumnAlignment
import io.github.r0x4nk.nexnote.util.DateUtils
import io.github.r0x4nk.nexnote.util.ImageFileManager
import io.github.r0x4nk.nexnote.util.MarkdownBlock
import io.github.r0x4nk.nexnote.util.MarkdownColors
import io.github.r0x4nk.nexnote.util.MarkdownParser
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

internal object PdfNoteExporter {
    fun write(
        file: File,
        notes: List<Note>,
        imageFileProvider: (String) -> File
    ) {
        PdfNoteDocumentWriter(file, notes, imageFileProvider).write()
    }
}

private class PdfNoteDocumentWriter(
    private val file: File,
    private val notes: List<Note>,
    private val imageFileProvider: (String) -> File
) {
    private val contentWidth = (PAGE_WIDTH - 2 * MARGIN_X).toInt()
    private val maxY = PAGE_HEIGHT - MARGIN_Y
    private val pageContentHeight = PAGE_HEIGHT - 2 * MARGIN_Y
    private val pdf = PdfDocument()
    private var pageNumber = 1
    private var page = pdf.startPage(newPageInfo())
    private var canvas = page.canvas
    private var y = MARGIN_Y

    private val titlePaint = TextPaint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = PdfColor.Text
        isAntiAlias = true
    }
    private val metaPaint = TextPaint().apply {
        textSize = 10f
        color = PdfColor.MutedText
        isAntiAlias = true
    }
    private val bodyPaint = TextPaint().apply {
        textSize = 12f
        color = PdfColor.Text
        isAntiAlias = true
    }
    private val quotePaint = TextPaint(bodyPaint).apply {
        color = PdfColor.MutedText
        textSkewX = -0.2f
    }
    private val codePaint = TextPaint(bodyPaint).apply {
        typeface = Typeface.MONOSPACE
        textSize = 11f
        color = PdfColor.CodeText
    }
    private val dividerPaint = Paint().apply {
        color = PdfColor.Divider
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val blockquoteBarPaint = Paint().apply {
        color = PdfColor.QuoteBar
        style = Paint.Style.FILL
    }
    private val codeBackgroundPaint = Paint().apply {
        color = PdfColor.CodeBackground
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val tableBorderPaint = Paint().apply {
        color = PdfColor.TableBorder
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val tableHeaderPaint = Paint().apply {
        color = PdfColor.TableHeaderBackground
        style = Paint.Style.FILL
    }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun write() {
        try {
            notes.forEachIndexed { index, note -> drawNote(index, note) }
            pdf.finishPage(page)
            FileOutputStream(file).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    private fun drawNote(index: Int, note: Note) {
        if (index > 0) drawDivider()

        val titleText = note.title.ifBlank { "(untitled)" }
        drawLayout(makeLayout(titleText, titlePaint))
        addVerticalSpace(4f)

        drawLayout(makeLayout(DateUtils.formatDateTime(note.creationDate), metaPaint))
        addVerticalSpace(10f)

        note.toPdfBlocks().forEach(::drawBlock)
    }

    private fun Note.toPdfBlocks(): List<MarkdownBlock> {
        if (content.isBlank()) return emptyList()
        return if (isMarkdown) {
            MarkdownParser.parseBlocks(content, PdfMarkdownColors)
        } else {
            listOf(MarkdownBlock.TextBlock(AnnotatedString(content)))
        }
    }

    private fun drawBlock(block: MarkdownBlock) {
        when (block) {
            is MarkdownBlock.TextBlock -> drawTextBlock(block.annotatedString)
            is MarkdownBlock.BlockquoteBlock -> drawBlockquote(block.content)
            is MarkdownBlock.CodeBlock -> drawCodeBlock(block.code)
            is MarkdownBlock.ImageBlock -> drawImageBlock(block)
            is MarkdownBlock.TableBlock -> drawTable(block)
            MarkdownBlock.HorizontalRuleBlock -> drawHorizontalRule()
        }
    }

    private fun drawTextBlock(text: AnnotatedString) {
        if (text.text.isEmpty()) return
        drawLayout(makeLayout(text.toSpannable(), bodyPaint))
        addVerticalSpace(BLOCK_SPACING)
    }

    private fun drawBlockquote(content: AnnotatedString) {
        if (content.text.isEmpty()) return
        val layout = makeLayout(
            text = content.toSpannable(),
            paint = quotePaint,
            width = contentWidth - QUOTE_TEXT_OFFSET.toInt()
        )
        drawLayout(
            layout = layout,
            x = MARGIN_X + QUOTE_TEXT_OFFSET,
            lineDecoration = { top, bottom ->
                canvas.drawRect(
                    MARGIN_X,
                    top,
                    MARGIN_X + QUOTE_BAR_WIDTH,
                    bottom,
                    blockquoteBarPaint
                )
            }
        )
        addVerticalSpace(BLOCK_SPACING)
    }

    private fun drawCodeBlock(code: String) {
        val displayCode = code.ifEmpty { " " }
        val layout = makeLayout(
            text = displayCode,
            paint = codePaint,
            width = contentWidth - (CODE_PADDING_X * 2).toInt()
        )
        drawBoxedLayout(
            layout = layout,
            x = MARGIN_X + CODE_PADDING_X,
            boxLeft = MARGIN_X,
            boxRight = PAGE_WIDTH - MARGIN_X,
            backgroundPaint = codeBackgroundPaint,
            cornerRadius = CODE_CORNER_RADIUS,
            paddingY = CODE_PADDING_Y
        )
        addVerticalSpace(BLOCK_SPACING)
    }

    private fun drawImageBlock(block: MarkdownBlock.ImageBlock) {
        val bitmap = decodeImage(block.path)
        if (bitmap == null) {
            drawMissingImage(block)
            return
        }

        try {
            val size = bitmap.scaledPdfSize()
            if (y + size.height > maxY) newPage()

            val left = MARGIN_X + (contentWidth - size.width) / 2f
            val dest = RectF(left, y, left + size.width, y + size.height)
            canvas.drawBitmap(bitmap, null, dest, imagePaint)
            y += size.height
            addVerticalSpace(IMAGE_SPACING)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeImage(relativePath: String): Bitmap? =
        runCatching {
            val file = imageFileProvider(relativePath)
            if (!file.exists()) return@runCatching null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val sampleSize = ImageFileManager.calculateSampleSize(
                longestSide = max(bounds.outWidth, bounds.outHeight)
            )
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        }.getOrNull()

    private fun Bitmap.scaledPdfSize(): PdfImageSize {
        val widthScale = contentWidth / width.toFloat()
        val fullWidthHeight = height * widthScale
        val heightScale = if (fullWidthHeight > pageContentHeight) {
            pageContentHeight / fullWidthHeight
        } else {
            1f
        }
        return PdfImageSize(
            width = contentWidth * heightScale,
            height = fullWidthHeight * heightScale
        )
    }

    private fun drawMissingImage(block: MarkdownBlock.ImageBlock) {
        val label = block.altText.ifBlank { block.path }
        val text = "[Image not found: $label]"
        drawLayout(makeLayout(text, metaPaint))
        addVerticalSpace(BLOCK_SPACING)
    }

    private fun drawTable(table: MarkdownBlock.TableBlock) {
        val columnCount = table.headers.size
        if (columnCount == 0) return

        drawTableRow(
            cells = table.headers,
            alignments = table.alignments,
            isHeader = true,
            columnCount = columnCount
        )
        table.rows.forEach { row ->
            drawTableRow(
                cells = row,
                alignments = table.alignments,
                isHeader = false,
                columnCount = columnCount
            )
        }
        addVerticalSpace(TABLE_SPACING)
    }

    private fun drawTableRow(
        cells: List<AnnotatedString>,
        alignments: List<ColumnAlignment>,
        isHeader: Boolean,
        columnCount: Int
    ) {
        val cellWidth = contentWidth / columnCount.toFloat()
        val textWidth = (cellWidth - TABLE_CELL_PADDING_X * 2).roundToInt().coerceAtLeast(1)
        val layouts = List(columnCount) { index ->
            val alignment = alignments.getOrElse(index) { ColumnAlignment.LEFT }.toLayoutAlignment()
            val paint = TextPaint(bodyPaint).apply {
                if (isHeader) isFakeBoldText = true
            }
            makeLayout(
                text = cells.getOrElse(index) { AnnotatedString("") }.toSpannable(),
                paint = paint,
                width = textWidth,
                alignment = alignment
            )
        }
        val rowHeight = layouts
            .maxOfOrNull { it.height.toFloat() + TABLE_CELL_PADDING_Y * 2 }
            ?.coerceAtLeast(MIN_TABLE_ROW_HEIGHT)
            ?: MIN_TABLE_ROW_HEIGHT

        if (y + rowHeight > maxY) newPage()

        var x = MARGIN_X
        layouts.forEach { layout ->
            val rect = RectF(x, y, x + cellWidth, y + rowHeight)
            if (isHeader) canvas.drawRect(rect, tableHeaderPaint)
            canvas.drawRect(rect, tableBorderPaint)

            canvas.save()
            canvas.translate(x + TABLE_CELL_PADDING_X, y + TABLE_CELL_PADDING_Y)
            layout.draw(canvas)
            canvas.restore()

            x += cellWidth
        }
        y += rowHeight
    }

    private fun drawHorizontalRule() {
        addVerticalSpace(8f)
        if (y + 1f > maxY) newPage()
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
        addVerticalSpace(12f)
    }

    private fun drawDivider() {
        addVerticalSpace(12f)
        if (y + 20f > maxY) newPage()
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
        addVerticalSpace(14f)
    }

    private fun drawLayout(
        layout: StaticLayout,
        x: Float = MARGIN_X,
        lineDecoration: ((top: Float, bottom: Float) -> Unit)? = null
    ) {
        for (lineIndex in 0 until layout.lineCount) {
            val lineTop = layout.getLineTop(lineIndex)
            val lineBottom = layout.getLineBottom(lineIndex)
            val lineHeight = (lineBottom - lineTop).toFloat()

            if (y + lineHeight > maxY) newPage()

            lineDecoration?.invoke(y, y + lineHeight)
            drawLayoutLine(layout, lineTop, lineBottom, x, y)
            y += lineHeight
        }
    }

    private fun drawBoxedLayout(
        layout: StaticLayout,
        x: Float,
        boxLeft: Float,
        boxRight: Float,
        backgroundPaint: Paint,
        cornerRadius: Float,
        paddingY: Float
    ) {
        val totalHeight = layout.height + paddingY * 2
        if (totalHeight <= pageContentHeight) {
            if (y + totalHeight > maxY) newPage()
            canvas.drawRoundRect(
                RectF(boxLeft, y, boxRight, y + totalHeight),
                cornerRadius,
                cornerRadius,
                backgroundPaint
            )
            canvas.save()
            canvas.translate(x, y + paddingY)
            layout.draw(canvas)
            canvas.restore()
            y += totalHeight
            return
        }

        drawBoxedLayoutLineByLine(layout, x, boxLeft, boxRight, backgroundPaint, paddingY)
    }

    private fun drawBoxedLayoutLineByLine(
        layout: StaticLayout,
        x: Float,
        boxLeft: Float,
        boxRight: Float,
        backgroundPaint: Paint,
        paddingY: Float
    ) {
        for (lineIndex in 0 until layout.lineCount) {
            val lineTop = layout.getLineTop(lineIndex)
            val lineBottom = layout.getLineBottom(lineIndex)
            val segmentHeight = (lineBottom - lineTop).toFloat() + paddingY * 2

            if (y + segmentHeight > maxY) newPage()

            canvas.drawRect(boxLeft, y, boxRight, y + segmentHeight, backgroundPaint)
            drawLayoutLine(layout, lineTop, lineBottom, x, y + paddingY)
            y += segmentHeight
        }
    }

    private fun drawLayoutLine(
        layout: StaticLayout,
        lineTop: Int,
        lineBottom: Int,
        x: Float,
        lineY: Float
    ) {
        canvas.save()
        canvas.translate(x, lineY - lineTop)
        canvas.clipRect(0f, lineTop.toFloat(), layout.width.toFloat(), lineBottom.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    private fun makeLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int = contentWidth,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()

    private fun addVerticalSpace(space: Float) {
        if (y + space > maxY) {
            newPage()
        } else {
            y += space
        }
    }

    private fun newPage() {
        pdf.finishPage(page)
        pageNumber++
        page = pdf.startPage(newPageInfo())
        canvas = page.canvas
        y = MARGIN_Y
    }

    private fun newPageInfo(): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_X = 50f
        private const val MARGIN_Y = 60f

        private const val BLOCK_SPACING = 8f
        private const val IMAGE_SPACING = 10f
        private const val TABLE_SPACING = 10f
        private const val QUOTE_BAR_WIDTH = 3f
        private const val QUOTE_TEXT_OFFSET = 12f
        private const val CODE_PADDING_X = 10f
        private const val CODE_PADDING_Y = 8f
        private const val CODE_CORNER_RADIUS = 5f
        private const val TABLE_CELL_PADDING_X = 6f
        private const val TABLE_CELL_PADDING_Y = 5f
        private const val MIN_TABLE_ROW_HEIGHT = 22f

        private val PdfMarkdownColors = MarkdownColors(
            linkColor = ComposeColor(0xFF1565C0),
            inlineCodeBackground = ComposeColor(0xFFECEFF1),
            inlineCodeForeground = ComposeColor(0xFF263238)
        )
    }
}

private data class PdfImageSize(
    val width: Float,
    val height: Float
)

private object PdfColor {
    const val Text = android.graphics.Color.BLACK
    const val MutedText = android.graphics.Color.DKGRAY
    const val Divider = android.graphics.Color.LTGRAY
    const val QuoteBar = 0xFF9E9E9E.toInt()
    const val CodeText = 0xFF263238.toInt()
    const val CodeBackground = 0xFFF4F6F8.toInt()
    const val TableBorder = 0xFFBDBDBD.toInt()
    const val TableHeaderBackground = 0xFFECEFF1.toInt()
}

private fun AnnotatedString.toSpannable(): SpannableString {
    val spannable = SpannableString(text)
    spanStyles.forEach { range ->
        spannable.applySpanStyle(range.item, range.start, range.end)
    }
    return spannable
}

private fun SpannableString.applySpanStyle(style: SpanStyle, start: Int, end: Int) {
    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(safeStart, length)
    if (safeStart == safeEnd) return

    applyTypefaceSpan(style, safeStart, safeEnd)
    if (style.fontFamily == FontFamily.Monospace) {
        setPdfSpan(TypefaceSpan("monospace"), safeStart, safeEnd)
    }
    if (style.textDecoration == TextDecoration.Underline) {
        setPdfSpan(UnderlineSpan(), safeStart, safeEnd)
    }
    if (style.textDecoration == TextDecoration.LineThrough) {
        setPdfSpan(StrikethroughSpan(), safeStart, safeEnd)
    }
    if (style.color != ComposeColor.Unspecified) {
        setPdfSpan(ForegroundColorSpan(style.color.toArgb()), safeStart, safeEnd)
    }
    if (style.background != ComposeColor.Unspecified) {
        setPdfSpan(BackgroundColorSpan(style.background.toArgb()), safeStart, safeEnd)
    }
    if (style.fontSize.isSpecified && style.fontSize.type == TextUnitType.Sp) {
        setPdfSpan(AbsoluteSizeSpan(style.fontSize.value.roundToInt(), true), safeStart, safeEnd)
    }
}

private fun SpannableString.applyTypefaceSpan(style: SpanStyle, start: Int, end: Int) {
    val isBold = (style.fontWeight?.weight ?: FontWeight.Normal.weight) >= FontWeight.Bold.weight
    val isItalic = style.fontStyle == FontStyle.Italic
    val typefaceStyle = when {
        isBold && isItalic -> Typeface.BOLD_ITALIC
        isBold -> Typeface.BOLD
        isItalic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    if (typefaceStyle != Typeface.NORMAL) {
        setPdfSpan(StyleSpan(typefaceStyle), start, end)
    }
}

private fun SpannableString.setPdfSpan(span: Any, start: Int, end: Int) {
    setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

private fun ColumnAlignment.toLayoutAlignment(): Layout.Alignment =
    when (this) {
        ColumnAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
        ColumnAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        ColumnAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }
