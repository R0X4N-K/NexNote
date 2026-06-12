package io.github.r0x4nk.nexnote.fileimport

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal class ExternalFileImporter(
    private val contentResolver: ContentResolver,
    private val saveNote: SaveNoteUseCase,
    private val indexNoteTags: IndexNoteTagsUseCase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun importFrom(intent: Intent?): ExternalFileImportResult =
        withContext(Dispatchers.IO) {
            if (!canHandle(intent)) return@withContext ExternalFileImportResult.Ignored

            val uri = intent?.data
                ?: return@withContext ExternalFileImportResult.Failed("File is not available")

            runCatching { importUri(uri) }
                .getOrElse { error ->
                    NexNoteDebugLog.persistence(
                        event = "externalFileImportFailed",
                        details = NexNoteDebugLog.throwableSummary(error)
                    )
                    val message = if (error is ImportedFileTooLargeException) {
                        "File is too large"
                    } else {
                        "Could not import file"
                    }
                    ExternalFileImportResult.Failed(message)
                }
        }

    private suspend fun importUri(uri: Uri): ExternalFileImportResult {
        val declaredSize = querySize(uri)
        if (declaredSize != null && declaredSize > TextFileImportParser.MAX_CONTENT_BYTES) {
            return ExternalFileImportResult.Failed("File is too large")
        }

        val displayName = queryDisplayName(uri) ?: Uri.decode(uri.lastPathSegment.orEmpty())
        val bytes = contentResolver.openInputStream(uri)
            ?.use { it.readBounded(TextFileImportParser.MAX_CONTENT_BYTES) }
            ?: return ExternalFileImportResult.Failed("File is not available")

        val importedFile = when (val parsed = TextFileImportParser.parse(displayName, bytes)) {
            is TextFileImportParseResult.Parsed -> parsed.file
            is TextFileImportParseResult.Rejected ->
                return ExternalFileImportResult.Failed(parsed.message)
        }

        val createdAt = nowMillis()
        val noteId = saveNote(
            Note(
                title = importedFile.title,
                content = importedFile.content,
                isMarkdown = true,
                creationDate = createdAt,
                lastModifiedDate = createdAt
            )
        )
        runCatching {
            indexNoteTags(noteId, importedFile.content)
        }.onFailure { error ->
            NexNoteDebugLog.persistence(
                event = "externalFileImportTagIndexFailed",
                details = "noteId=$noteId ${NexNoteDebugLog.throwableSummary(error)}"
            )
        }

        return ExternalFileImportResult.Imported(noteId)
    }

    private fun canHandle(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val scheme = intent.data?.scheme ?: return false
        return scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE
    }

    private fun queryDisplayName(uri: Uri): String? =
        queryOpenableColumn(uri, OpenableColumns.DISPLAY_NAME)

    private fun querySize(uri: Uri): Long? =
        queryOpenableColumn(uri, OpenableColumns.SIZE)?.toLongOrNull()

    private fun queryOpenableColumn(uri: Uri, column: String): String? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null

        return contentResolver.query(uri, arrayOf(column), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val columnIndex = cursor.getColumnIndex(column)
                if (columnIndex < 0 || cursor.isNull(columnIndex)) {
                    null
                } else {
                    cursor.getString(columnIndex)
                }
            }
    }

    private fun InputStream.readBounded(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0

        while (true) {
            val read = read(buffer)
            if (read == -1) return output.toByteArray()
            if (read > maxBytes - total) throw ImportedFileTooLargeException()

            output.write(buffer, 0, read)
            total += read
        }
    }
}

private class ImportedFileTooLargeException : Exception()

