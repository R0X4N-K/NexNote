package io.github.r0x4nk.nexnote.util

import android.util.Log
import io.github.r0x4nk.nexnote.BuildConfig
import io.github.r0x4nk.nexnote.domain.model.Note

/**
 * Structured diagnostics that deliberately exclude note text and exception
 * messages. User-authored content must never be emitted to logcat, including
 * in debug builds.
 *
 * Keep all events under one tag so a single `adb logcat` filter captures the
 * full path from Compose text input to Room persistence.
 */
object NexNoteDebugLog {
    const val TAG = "NexNoteDebug"
    val isEnabled: Boolean
        get() = BuildConfig.DEBUG

    fun editor(event: String, details: String = "") {
        write(layer = "EDITOR", event = event, details = details)
    }

    fun editor(event: String, details: () -> String) {
        write(layer = "EDITOR", event = event, details = details)
    }

    fun viewModel(event: String, details: String = "") {
        write(layer = "VIEWMODEL", event = event, details = details)
    }

    fun viewModel(event: String, details: () -> String) {
        write(layer = "VIEWMODEL", event = event, details = details)
    }

    fun persistence(event: String, details: String = "") {
        write(layer = "PERSISTENCE", event = event, details = details)
    }

    fun persistence(event: String, details: () -> String) {
        write(layer = "PERSISTENCE", event = event, details = details)
    }

    fun repository(event: String, details: String = "") {
        write(layer = "REPOSITORY", event = event, details = details)
    }

    fun repository(event: String, details: () -> String) {
        write(layer = "REPOSITORY", event = event, details = details)
    }

    fun repositoryWarning(event: String, details: () -> String) {
        write(
            layer = "REPOSITORY",
            event = event,
            details = details,
            priority = Log.WARN
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun textSummary(label: String, text: String, redact: Boolean = false): String {
        if (!isEnabled) return ""
        return "$label.len=${text.length} $label.content=redacted"
    }

    fun noteSummary(label: String, note: Note?): String {
        if (!isEnabled) return ""
        if (note == null) return "$label=null"
        return buildString {
            append("$label.id=${note.id} ")
            append("$label.vault=${note.isInVault} ")
            append("$label.titleLen=${note.title.length} ")
            append("$label.contentLen=${note.content.length} ")
            append("$label.markdown=${note.isMarkdown} ")
            append("$label.preview=${note.isPreviewMode} ")
            append("$label.modified=${note.lastModifiedDate} ")
            append("$label.content=redacted")
        }
    }

    fun throwableSummary(error: Throwable): String {
        if (!isEnabled) return ""
        return "error=${error::class.java.simpleName} message=redacted"
    }

    private fun write(
        layer: String,
        event: String,
        details: String,
        priority: Int = Log.DEBUG
    ) {
        if (!isEnabled) return

        log(priority, buildMessage(layer = layer, event = event, details = details))
    }

    private fun write(
        layer: String,
        event: String,
        details: () -> String,
        priority: Int = Log.DEBUG
    ) {
        if (!isEnabled) return

        log(priority, buildMessage(layer = layer, event = event, details = details()))
    }

    private fun buildMessage(layer: String, event: String, details: String): String {
        val message = buildString {
            append("layer=").append(layer)
            append(" event=").append(event)
            append(" thread=\"").append(Thread.currentThread().name).append('"')
            if (details.isNotBlank()) {
                append(' ').append(details)
            }
        }
        return message
    }

    private fun log(priority: Int, message: String) {
        runCatching {
            when (priority) {
                Log.WARN -> Log.w(TAG, message)
                else -> Log.d(TAG, message)
            }
        }
    }

}
