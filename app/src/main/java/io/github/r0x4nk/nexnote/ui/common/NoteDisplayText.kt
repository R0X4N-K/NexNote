package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.runtime.Immutable
import io.github.r0x4nk.nexnote.domain.model.Note

private const val NOTE_LABEL_MAX_LENGTH = 80
private const val UNTITLED_NOTE_LABEL = "Untitled note"
private val whitespaceRegex = Regex("\\s+")

@Immutable
data class TrashedNoteEvent(
    val noteId: Long,
    val noteLabel: String,
    val additionalNoteIds: List<Long> = emptyList()
) {
    val noteIds: List<Long>
        get() = listOf(noteId) + additionalNoteIds
}

internal fun TrashedNoteEvent.snackbarMessage(): String =
    if (noteIds.size == 1) {
        "Moved \"$noteLabel\" to trash"
    } else {
        "Moved ${noteIds.size} notes to trash"
    }

internal fun Note.toTrashedNoteEvent(): TrashedNoteEvent =
    TrashedNoteEvent(noteId = id, noteLabel = displayLabel())

internal fun Collection<Note>.toTrashedNoteEvent(): TrashedNoteEvent? {
    val notes = filter { it.id > 0L }
    if (notes.isEmpty()) return null
    val first = notes.first()
    return TrashedNoteEvent(
        noteId = first.id,
        noteLabel = first.displayLabel(),
        additionalNoteIds = notes.drop(1).map { it.id }
    )
}

internal fun Note.displayLabel(maxLength: Int = NOTE_LABEL_MAX_LENGTH): String {
    val rawLabel = title.cleanOneLine().ifBlank {
        content.firstMeaningfulLine() ?: UNTITLED_NOTE_LABEL
    }
    return rawLabel.take(maxLength)
}

private fun String.firstMeaningfulLine(): String? =
    lineSequence()
        .map { line -> line.cleanOneLine() }
        .firstOrNull { line -> line.isNotBlank() }

private fun String.cleanOneLine(): String =
    trim().replace(whitespaceRegex, " ")
