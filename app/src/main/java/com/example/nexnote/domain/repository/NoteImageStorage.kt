package com.example.nexnote.domain.repository

import java.io.File
import java.io.InputStream

/**
 * Storage boundary for note images.
 *
 * Paths returned from this API are relative paths suitable for persistence in
 * Note.imagePaths and Markdown image tags.
 */
interface NoteImageStorage {
    suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String

    suspend fun deleteImage(relativePath: String): Boolean

    fun getImageFile(relativePath: String): File
}
