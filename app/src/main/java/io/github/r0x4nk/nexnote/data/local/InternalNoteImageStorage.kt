package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.util.ImageFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
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
        val input = openInputStream()
            ?: throw IOException("Image input stream is unavailable")

        try {
            input.use { source ->
                destination.outputStream().use { output -> source.copyTo(output) }
            }
            if (destination.length() == 0L) {
                throw IOException("Copied image is empty")
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
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
