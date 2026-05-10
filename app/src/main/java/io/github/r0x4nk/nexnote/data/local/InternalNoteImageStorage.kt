package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.util.NoteImageProcessor
import io.github.r0x4nk.nexnote.util.ImageFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Persists note images in the app's internal storage directory.
 *
 * @param filesDir          root directory for internal file storage.
 * @param currentTimeMillis clock function, injectable for deterministic tests.
 * @param processImage      strategy that copies (and optionally transforms) an
 *                          image stream into a destination file. Defaults to
 *                          [NoteImageProcessor.processAndSave] which handles
 *                          EXIF correction and downsampling. Tests may supply a
 *                          raw-copy lambda to avoid Android framework dependencies.
 */
class InternalNoteImageStorage(
    private val filesDir: File,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val processImage: (inputStreamProvider: () -> InputStream?, destination: File) -> Unit =
        NoteImageProcessor::processAndSave
) : NoteImageStorage {

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String = withContext(Dispatchers.IO) {
        ImageFileManager.ensureImageDir(filesDir)
        val relativePath = buildUniqueRelativePath(noteId)
        val destination = ImageFileManager.getImageFile(filesDir, relativePath)

        try {
            processImage(openInputStream, destination)
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

    private fun buildUniqueRelativePath(noteId: Long): String {
        val baseTimestamp = currentTimeMillis()
        var offset = 0L
        while (true) {
            val relativePath = ImageFileManager.buildRelativePath(noteId, baseTimestamp + offset)
            if (!ImageFileManager.getImageFile(filesDir, relativePath).exists()) {
                return relativePath
            }
            offset++
        }
    }
}
