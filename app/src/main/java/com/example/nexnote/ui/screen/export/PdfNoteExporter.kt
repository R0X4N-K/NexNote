package com.example.nexnote.ui.screen.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.nexnote.domain.model.Note
import com.example.nexnote.util.DateUtils
import java.io.File
import java.io.FileOutputStream

internal object PdfNoteExporter {
    fun write(file: File, notes: List<Note>) {
        PdfNoteDocumentWriter(file, notes).write()
    }
}

private class PdfNoteDocumentWriter(
    private val file: File,
    private val notes: List<Note>
) {
    private val contentWidth = (PAGE_WIDTH - 2 * MARGIN_X).toInt()
    private val maxY = PAGE_HEIGHT - MARGIN_Y
    private val pdf = PdfDocument()
    private var pageNumber = 1
    private var page = pdf.startPage(newPageInfo())
    private var canvas = page.canvas
    private var y = MARGIN_Y

    private val titlePaint = TextPaint().apply {
        textSize = 16f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true
    }
    private val metaPaint = TextPaint().apply {
        textSize = 10f; color = Color.DKGRAY; isAntiAlias = true
    }
    private val bodyPaint = TextPaint().apply {
        textSize = 12f; color = Color.BLACK; isAntiAlias = true
    }
    private val dividerPaint = Paint().apply {
        color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE
    }

    fun write() {
        notes.forEachIndexed { index, note -> drawNote(index, note) }
        pdf.finishPage(page)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
    }

    private fun drawNote(index: Int, note: Note) {
        if (index > 0) drawDivider()

        val titleText = note.title.ifBlank { "(untitled)" }
        drawLayout(makeLayout(titleText, titlePaint))
        y += 4f

        val dateText = DateUtils.formatDateTime(note.creationDate)
        drawLayout(makeLayout(dateText, metaPaint))
        y += 6f

        if (note.content.isNotBlank()) {
            drawLayout(makeLayout(note.content, bodyPaint))
        }
    }

    private fun drawDivider() {
        y += 8f
        if (y + 20f > maxY) newPage()
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
        y += 12f
    }

    private fun drawLayout(layout: StaticLayout) {
        for (lineIndex in 0 until layout.lineCount) {
            val lineTop = layout.getLineTop(lineIndex)
            val lineBottom = layout.getLineBottom(lineIndex)
            val lineHeight = (lineBottom - lineTop).toFloat()

            if (y + lineHeight > maxY) newPage()

            canvas.save()
            canvas.translate(MARGIN_X, y - lineTop)
            canvas.clipRect(0f, lineTop.toFloat(), contentWidth.toFloat(), lineBottom.toFloat())
            layout.draw(canvas)
            canvas.restore()

            y += lineHeight
        }
    }

    private fun makeLayout(text: String, paint: TextPaint): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()

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
    }
}
