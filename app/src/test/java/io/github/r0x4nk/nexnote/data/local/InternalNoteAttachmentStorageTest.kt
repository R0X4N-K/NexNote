package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.domain.repository.copyStoredNoteFile
import io.github.r0x4nk.nexnote.util.ImageFileManager
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InternalNoteAttachmentStorageTest {
    @get:Rule val folder = TemporaryFolder()
    private fun files() = java.io.File(folder.root, "images").walkTopDown().filter { it.isFile }.toList()

    @Test fun `copies exact bytes closes stream and preserves only safe extension`() = runTest {
        val bytes = ByteArray(16_000) { it.toByte() }
        var closed = false
        val path = InternalNoteAttachmentStorage(folder.root).copyAttachmentToInternal(42, "../../Secret report.PDF") {
            object : ByteArrayInputStream(bytes) { override fun close() { closed = true; super.close() } }
        }
        assertTrue(closed)
        assertTrue(NoteAttachment.isAttachmentPath(path))
        assertTrue(path.endsWith(".pdf"))
        assertFalse(path.contains("Secret"))
        assertArrayEquals(bytes, ImageFileManager.getImageFile(folder.root, path).readBytes())
    }

    @Test fun `simultaneous imports never overwrite each other`() = runTest {
        val storage = InternalNoteAttachmentStorage(folder.root)
        val paths = (1..20).map { value -> async {
            storage.copyAttachmentToInternal(1, "same.pdf") { "$value".byteInputStream() }
        } }.awaitAll()
        assertEquals(20, paths.toSet().size)
        assertEquals(20, files().size)
    }

    @Test fun `large stream is imported without a document quota`() = runTest {
        val size = 20L * 1024 * 1024
        var remaining = size
        var closed = false
        val path = InternalNoteAttachmentStorage(folder.root).copyAttachmentToInternal(1, "large.zip") {
            object : InputStream() {
                override fun read(): Int = if (remaining-- > 0) 7 else -1
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    if (remaining == 0L) return -1
                    assertTrue(length <= 8192)
                    val count = minOf(length.toLong(), remaining).toInt()
                    buffer.fill(7, offset, offset + count)
                    remaining -= count
                    return count
                }
                override fun close() { closed = true }
            }
        }
        assertTrue(closed)
        assertEquals(size, ImageFileManager.getImageFile(folder.root, path).length())
    }

    @Test fun `read failure cancellation and missing provider leave no file`() = runTest {
        val storage = InternalNoteAttachmentStorage(folder.root)
        for (error in listOf(IOException("disconnected"), CancellationException("cancelled"))) {
            val result = runCatching { storage.copyAttachmentToInternal(1, "a.pdf") {
                object : InputStream() { override fun read(): Int = throw error }
            } }
            assertEquals(error.javaClass, result.exceptionOrNull()?.javaClass)
            assertEquals(error.message, result.exceptionOrNull()?.message)
            assertTrue(files().isEmpty())
        }
        assertTrue(runCatching { storage.copyAttachmentToInternal(1, "a.pdf") { null } }.isFailure)
        assertTrue(files().isEmpty())
    }

    @Test fun `byte preserving duplication never invokes image decoder and owns independent file`() = runTest {
        val storage = InternalNoteImageStorage(folder.root, processImage = { _, _ -> error("Must not decode a PDF") })
        val source = storage.copyAttachmentToInternal(1, "a.pdf") { "%PDF-test".byteInputStream() }
        val duplicate = storage.copyStoredNoteFile(2, source) { storage.getImageFile(source).inputStream() }
        assertNotEquals(source, duplicate)
        storage.deleteImage(source)
        assertEquals("%PDF-test", storage.getImageFile(duplicate).readText())
        storage.deleteImage(duplicate)
        assertTrue(files().isEmpty())
    }

    @Test fun `empty files are valid attachments and invalid owner is rejected`() = runTest {
        val storage = InternalNoteAttachmentStorage(folder.root)
        val path = storage.copyAttachmentToInternal(1, "empty.txt") { byteArrayOf().inputStream() }
        assertEquals(0L, ImageFileManager.getImageFile(folder.root, path).length())
        assertTrue(runCatching { storage.copyAttachmentToInternal(0, "bad") { null } }.isFailure)
    }
}
