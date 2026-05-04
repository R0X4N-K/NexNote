package com.example.nexnote.data.local

import com.example.nexnote.domain.repository.NoteImageStorage
import com.example.nexnote.util.ImageFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class InternalNoteImageStorage(
    private val filesDir: File,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : NoteImageStorage {

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String = withContext(Dispatchers.IO) {
        ImageFileManager.ensureImageDir(filesDir)
        val relativePath = ImageFileManager.buildRelativePath(noteId, currentTimeMillis())
        val destination = ImageFileManager.getImageFile(filesDir, relativePath)
        openInputStream()?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        relativePath
    }

    override suspend fun deleteImage(relativePath: String): Boolean =
        withContext(Dispatchers.IO) {
            ImageFileManager.deleteImage(filesDir, relativePath)
        }

    override fun getImageFile(relativePath: String): File =
        ImageFileManager.getImageFile(filesDir, relativePath)
}
