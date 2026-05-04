package io.github.r0x4nk.nexnote.testing

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import java.io.File
import java.io.InputStream

class NoOpNoteImageStorage : NoteImageStorage {
    val deletedPaths = mutableListOf<String>()

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String = "images/note_${noteId}_img_0.jpg"

    override suspend fun deleteImage(relativePath: String): Boolean {
        deletedPaths += relativePath
        return true
    }

    override fun getImageFile(relativePath: String): File = File(relativePath)
}
