package io.github.r0x4nk.nexnote.domain.repository

import java.io.InputStream

/** Copies opaque bytes without decoding or recompressing them. The caller owns the returned path. */
interface NoteAttachmentStorage {
    suspend fun copyAttachmentToInternal(
        noteId: Long,
        fileName: String,
        openInputStream: () -> InputStream?
    ): String
}
