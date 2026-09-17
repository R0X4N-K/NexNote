package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import java.io.InputStream

/** Routes legacy image transforms and byte-preserving attachment copies explicitly. */
suspend fun NoteImageStorage.copyStoredNoteFile(
    noteId: Long,
    sourcePath: String,
    openInputStream: () -> InputStream?
): String = if (NoteAttachment.isAttachmentPath(sourcePath)) {
    check(this is NoteAttachmentStorage) { "Attachment storage is unavailable" }
    copyAttachmentToInternal(noteId, sourcePath.substringAfterLast('/'), openInputStream)
} else {
    copyImageToInternal(noteId, openInputStream)
}
