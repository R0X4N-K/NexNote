package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.DateUtils
import io.github.r0x4nk.nexnote.util.MarkdownPlainText

internal object ExportNoteFormatter {

    fun toPlainText(notes: List<Note>): String =
        notes.joinToString(separator = NOTE_SEPARATOR) { note ->
            buildString {
                appendPlainHeader(note)
                appendRenderedBody(note)
            }.trimEnd()
        }

    fun toMarkdown(notes: List<Note>): String =
        notes.joinToString(separator = NOTE_SEPARATOR) { note ->
            buildString {
                if (note.title.isNotBlank()) {
                    appendLine("# ${note.title}")
                    appendLine()
                }
                appendLine("_${DateUtils.formatDateTime(note.creationDate)}_")
                appendLine()
                append(note.content)
            }.trimEnd()
        }

    private fun StringBuilder.appendPlainHeader(note: Note) {
        if (note.title.isNotBlank()) {
            appendLine(note.title)
            appendLine()
        }
        appendLine(DateUtils.formatDateTime(note.creationDate))
        appendLine()
    }

    private fun StringBuilder.appendRenderedBody(note: Note) {
        val body = if (note.isMarkdown) {
            MarkdownPlainText.fromMarkdown(note.content)
        } else {
            note.content
        }
        append(body)
    }

    private const val NOTE_SEPARATOR = "\n\n---\n\n"
}
