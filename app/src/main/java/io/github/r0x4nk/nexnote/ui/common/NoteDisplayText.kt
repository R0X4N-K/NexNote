package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.runtime.Immutable
import io.github.r0x4nk.nexnote.domain.model.Note

private const val NOTE_LABEL_MAX_LENGTH = 80

@Immutable
data class TrashedNoteEvent(
    val noteId: Long,
    val noteLabel: String,
    val additionalNoteIds: List<Long> = emptyList()
) {
    val noteIds: List<Long>
        get() = listOf(noteId) + additionalNoteIds
}

internal fun Note.toTrashedNoteEvent(untitledLabel: String): TrashedNoteEvent =
    TrashedNoteEvent(noteId = id, noteLabel = displayLabel(untitledLabel = untitledLabel))

internal fun Collection<Note>.toTrashedNoteEvent(untitledLabel: String): TrashedNoteEvent? {
    val notes = filter { it.id > 0L }
    if (notes.isEmpty()) return null
    val first = notes.first()
    return TrashedNoteEvent(
        noteId = first.id,
        noteLabel = first.displayLabel(untitledLabel = untitledLabel),
        additionalNoteIds = notes.drop(1).map { it.id }
    )
}

internal fun trashedNoteEventForIds(
    noteIds: Collection<Long>,
    untitledLabel: String
): TrashedNoteEvent? {
    val ids = noteIds.asSequence().filter { it > 0L }.distinct().toList()
    if (ids.isEmpty()) return null
    return TrashedNoteEvent(
        noteId = ids.first(),
        noteLabel = untitledLabel,
        additionalNoteIds = ids.drop(1)
    )
}

internal fun Note.displayLabel(
    maxLength: Int = NOTE_LABEL_MAX_LENGTH,
    untitledLabel: String
): String {
    require(maxLength >= 0)
    if (maxLength == 0) return ""
    return title.compactLabel(maxLength, firstLineOnly = false)
        .ifEmpty { content.compactLabel(maxLength, firstLineOnly = true) }
        .ifEmpty { untitledLabel.take(maxLength) }
}

/** Stops at the visible prefix instead of allocating or normalizing the entire note. */
private fun String.compactLabel(maxLength: Int, firstLineOnly: Boolean): String {
    val result = StringBuilder(minOf(maxLength, length))
    var pendingSpace = false
    for (character in this) {
        if (firstLineOnly && result.isNotEmpty() && (character == '\n' || character == '\r')) break
        if (character.isWhitespace()) {
            pendingSpace = result.isNotEmpty()
        } else {
            if (pendingSpace) {
                result.append(' ')
                if (result.length == maxLength) break
            }
            result.append(character)
            if (result.length == maxLength) break
            pendingSpace = false
        }
    }
    return result.toString()
}