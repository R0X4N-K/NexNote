package io.github.r0x4nk.nexnote.data.local

import kotlinx.coroutines.NonCancellable
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.domain.repository.NoteAttachmentStorage
import io.github.r0x4nk.nexnote.util.ImageFileManager
import io.github.r0x4nk.nexnote.util.copyStreaming
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class InternalNoteAttachmentStorage(private val filesDir: File) : NoteAttachmentStorage {
    override suspend fun copyAttachmentToInternal(
        noteId: Long,
        fileName: String,
        openInputStream: () -> InputStream?
    ): String {
        require(noteId > 0)
        var destination: File? = null
        var partial: File? = null
        var marker: File? = null
        try {
            return withContext(Dispatchers.IO) {
                ImageFileManager.ensureImageDir(filesDir)
                val path = "${NoteAttachment.DIRECTORY}/note_${noteId}_${UUID.randomUUID()}." +
                    NoteAttachment.safeExtension(fileName)
                val target = ImageFileManager.getImageFile(filesDir, path)
                val parent = requireNotNull(target.parentFile)
                if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) throw IOException("Cannot create attachment directory")
                marker = AttachmentImportJournal(filesDir).begin(path)
                val staging = File(parent, "${target.name}.part")
                if (!staging.createNewFile()) throw IOException("Attachment staging already exists")
                partial = staging
                val input = openInputStream() ?: throw IOException("Cannot read attachment")
                input.use { source ->
                    staging.outputStream().use { output ->
                        copyStreaming(source, output, coroutineContext::ensureActive)
                        output.fd.sync()
                    }
                }
                coroutineContext.ensureActive()
                // Publish only a complete, synced file. Both paths reside on the same filesystem.
                Files.move(staging.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                destination = target
                path
            }
        } catch (error: Throwable) {
            // Also covers cancellation during dispatch back to the caller, before path ownership transfers.
            withContext(NonCancellable + Dispatchers.IO) {
                val targetRemoved = destination?.let { !it.exists() || it.delete() } ?: true
                val partialRemoved = partial?.let { !it.exists() || it.delete() } ?: true
                if (targetRemoved && partialRemoved) marker?.delete()
            }
            throw error
        }
    }
}
