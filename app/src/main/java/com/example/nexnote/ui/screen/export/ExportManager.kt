package com.example.nexnote.ui.screen.export

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.nexnote.domain.model.Note
import com.example.nexnote.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates and shares exported note files.
 * All I/O operations are dispatched to [Dispatchers.IO] internally.
 * No business logic here — it only consumes the [Note] list already filtered by the ViewModel.
 */
class ExportManager(private val context: Context) {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates the export file on IO and returns a ready-to-launch ACTION_SEND [Intent].
     * The intent is not launched internally because the UI needs an Activity context
     * to call startActivity.
     */
    suspend fun buildShareIntent(notes: List<Note>, format: ExportFormat): Intent =
        withContext(Dispatchers.IO) {
            val file = createExportFile(notes, format)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

    /**
     * Generates the PDF on IO and initiates native printing via [PrintManager].
     * Must be called from a coroutine launched in the UI (Main) context.
     */
    suspend fun print(notes: List<Note>) {
        val file = withContext(Dispatchers.IO) { createExportFile(notes, ExportFormat.PDF) }
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        manager.print(
            buildFileName(notes, ExportFormat.PDF),
            PdfPrintAdapter(file),
            null
        )
    }

    // ── Format routing ────────────────────────────────────────────────────────

    private fun createExportFile(notes: List<Note>, format: ExportFormat): File {
        val file = prepareFile(buildFileName(notes, format))
        when (format) {
            ExportFormat.TXT -> writeTxt(file, notes)
            ExportFormat.MD -> writeMd(file, notes)
            ExportFormat.PDF,
            ExportFormat.PRINT -> writePdf(file, notes)
        }
        return file
    }

    // ── File naming ───────────────────────────────────────────────────────────

    private fun buildFileName(notes: List<Note>, format: ExportFormat): String {
        val ext = format.extension
        val base = when {
            notes.size == 1 -> sanitize(notes.first().title.ifBlank { "Note" })
            else -> {
                val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                "NexNote_Export_$ts"
            }
        }
        return "$base.$ext"
    }

    /** Strips characters that are invalid in file names and truncates to 100 characters. */
    private fun sanitize(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().take(100).ifBlank { "Note" }

    private fun prepareFile(name: String): File {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        return File(dir, name)
    }

    // ── TXT writer ────────────────────────────────────────────────────────────

    private fun writeTxt(file: File, notes: List<Note>) {
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            notes.forEachIndexed { idx, note ->
                if (idx > 0) {
                    w.newLine(); w.appendLine("---"); w.newLine()
                }
                if (note.title.isNotBlank()) {
                    w.appendLine(note.title); w.newLine()
                }
                w.appendLine(DateUtils.formatDateTime(note.creationDate))
                w.newLine()
                w.appendLine(note.content)
            }
        }
    }

    // ── Markdown writer ───────────────────────────────────────────────────────

    private fun writeMd(file: File, notes: List<Note>) {
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            notes.forEachIndexed { idx, note ->
                if (idx > 0) {
                    w.newLine(); w.appendLine("---"); w.newLine()
                }
                if (note.title.isNotBlank()) {
                    w.appendLine("# ${note.title}"); w.newLine()
                }
                w.appendLine("_${DateUtils.formatDateTime(note.creationDate)}_")
                w.newLine()
                w.appendLine(note.content)
            }
        }
    }

    // ── PDF writer (line-by-line pagination) ─────────────────────────────────

    private fun writePdf(file: File, notes: List<Note>) {
        PdfNoteExporter.write(file, notes)
    }

    // ── PrintDocumentAdapter ──────────────────────────────────────────────────

    private class PdfPrintAdapter(private val pdfFile: File) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(pdfFile.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            try {
                pdfFile.inputStream().use { input ->
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    // ── Private extensions ────────────────────────────────────────────────────

    private val ExportFormat.extension: String
        get() = when (this) {
            ExportFormat.TXT -> "txt"
            ExportFormat.MD -> "md"
            ExportFormat.PDF,
            ExportFormat.PRINT -> "pdf"
        }

    private val ExportFormat.mimeType: String
        get() = when (this) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.MD -> "text/markdown"
            ExportFormat.PDF,
            ExportFormat.PRINT -> "application/pdf"
        }
}
