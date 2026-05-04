package com.example.nexnote.util

import android.util.Log
import com.example.nexnote.domain.model.Note

/**
 * Temporary structured diagnostics for the editor data-loss investigation.
 *
 * Keep all events under one tag so a single `adb logcat` filter captures the
 * full path from Compose text input to Room persistence.
 */
object NexNoteDebugLog {
    const val TAG = "NexNoteDebug"
    private const val ENABLED = true
    private const val SAMPLE_LIMIT = 120

    fun editor(event: String, details: String = "") {
        write(layer = "EDITOR", event = event, details = details)
    }

    fun viewModel(event: String, details: String = "") {
        write(layer = "VIEWMODEL", event = event, details = details)
    }

    fun persistence(event: String, details: String = "") {
        write(layer = "PERSISTENCE", event = event, details = details)
    }

    fun repository(event: String, details: String = "") {
        write(layer = "REPOSITORY", event = event, details = details)
    }

    fun textSummary(label: String, text: String): String {
        return "$label.len=${text.length} " +
            "$label.hash=${text.hashCode()} " +
            "$label.sample=\"${text.debugSample()}\""
    }

    fun noteSummary(label: String, note: Note?): String {
        if (note == null) return "$label=null"
        return "$label.id=${note.id} " +
            "$label.titleLen=${note.title.length} " +
            "$label.contentLen=${note.content.length} " +
            "$label.contentHash=${note.content.hashCode()} " +
            "$label.markdown=${note.isMarkdown} " +
            "$label.preview=${note.isPreviewMode} " +
            "$label.modified=${note.lastModifiedDate} " +
            "$label.contentSample=\"${note.content.debugSample()}\""
    }

    fun throwableSummary(error: Throwable): String {
        return "error=${error::class.java.simpleName} message=\"${error.message.orEmpty().debugSample()}\""
    }

    private fun write(layer: String, event: String, details: String) {
        if (!ENABLED) return

        val message = buildString {
            append("layer=").append(layer)
            append(" event=").append(event)
            append(" thread=\"").append(Thread.currentThread().name).append('"')
            if (details.isNotBlank()) {
                append(' ').append(details)
            }
        }
        runCatching { Log.d(TAG, message) }
    }

    private fun String.debugSample(): String {
        return take(SAMPLE_LIMIT)
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
    }
}
