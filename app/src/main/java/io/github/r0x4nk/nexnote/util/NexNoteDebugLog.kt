package io.github.r0x4nk.nexnote.util

import android.util.Log
import io.github.r0x4nk.nexnote.BuildConfig
import io.github.r0x4nk.nexnote.domain.model.Note

/**
 * Temporary structured diagnostics for the editor data-loss investigation.
 *
 * Keep all events under one tag so a single `adb logcat` filter captures the
 * full path from Compose text input to Room persistence.
 */
object NexNoteDebugLog {
    const val TAG = "NexNoteDebug"
    private const val SAMPLE_LIMIT = 120
    private const val FULL_HASH_LIMIT = 8_192
    private const val PARTIAL_HASH_SAMPLE_LIMIT = 512
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

    fun textSummary(label: String, text: String, redact: Boolean = false): String {
        if (!isEnabled) return ""
        if (redact) {
            return "$label.len=${text.length} $label.content=redacted"
        }
        return "$label.len=${text.length} " +
            "$label.hash=${text.debugFingerprint()} " +
            "$label.sample=\"${text.debugSample()}\""
    }

    fun noteSummary(label: String, note: Note?): String {
        if (!isEnabled) return ""
        if (note == null) return "$label=null"
        return buildString {
            append("$label.id=${note.id} ")
            append("$label.vault=${note.isInVault} ")
            if (note.isInVault) {
                append("$label.content=redacted")
            } else {
                append("$label.titleLen=${note.title.length} ")
                append("$label.contentLen=${note.content.length} ")
                append("$label.contentHash=${note.content.debugFingerprint()} ")
                append("$label.markdown=${note.isMarkdown} ")
                append("$label.preview=${note.isPreviewMode} ")
                append("$label.modified=${note.lastModifiedDate} ")
                append("$label.contentSample=\"${note.content.debugSample()}\"")
            }
        }
    }

    fun throwableSummary(error: Throwable): String {
        if (!isEnabled) return ""
        return "error=${error::class.java.simpleName} message=\"${error.message.orEmpty().debugSample()}\""
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

    private fun String.debugSample(): String {
        return take(SAMPLE_LIMIT)
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
    }

    private fun String.debugFingerprint(): String {
        if (length <= FULL_HASH_LIMIT) return hashCode().toString()

        val headHash = take(PARTIAL_HASH_SAMPLE_LIMIT).hashCode()
        val tailHash = takeLast(PARTIAL_HASH_SAMPLE_LIMIT).hashCode()
        return "partial:$headHash:$tailHash"
    }
}
