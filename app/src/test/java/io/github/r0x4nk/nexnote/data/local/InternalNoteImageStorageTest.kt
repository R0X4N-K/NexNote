package io.github.r0x4nk.nexnote.data.local

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException

class InternalNoteImageStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `copyImageToInternal writes stream to generated relative path`() = runTest {
        val storage = InternalNoteImageStorage(
            filesDir = tempFolder.root,
            currentTimeMillis = { 123L }
        )

        val relativePath = storage.copyImageToInternal(noteId = 42L) {
            ByteArrayInputStream("image bytes".toByteArray())
        }

        val file = storage.getImageFile(relativePath)
        assertEquals("images/note_42_img_123.jpg", relativePath)
        assertTrue(file.exists())
        assertEquals("image bytes", file.readText())
    }

    @Test
    fun `copyImageToInternal fails when stream is null`() = runTest {
        val storage = InternalNoteImageStorage(
            filesDir = tempFolder.root,
            currentTimeMillis = { 456L }
        )

        val result = runCatching {
            storage.copyImageToInternal(noteId = 7L) { null }
        }

        assertTrue(result.exceptionOrNull() is IOException)
        assertFalse(storage.getImageFile("images/note_7_img_456.jpg").exists())
    }

    @Test
    fun `copyImageToInternal removes empty destination on empty stream`() = runTest {
        val storage = InternalNoteImageStorage(
            filesDir = tempFolder.root,
            currentTimeMillis = { 789L }
        )

        val result = runCatching {
            storage.copyImageToInternal(noteId = 8L) {
                ByteArrayInputStream(ByteArray(0))
            }
        }

        assertTrue(result.exceptionOrNull() is IOException)
        assertFalse(storage.getImageFile("images/note_8_img_789.jpg").exists())
    }

    @Test
    fun `deleteImage removes only requested image`() = runTest {
        val storage = InternalNoteImageStorage(
            filesDir = tempFolder.root,
            currentTimeMillis = { 1L }
        )
        val keepPath = "images/note_1_img_100.jpg"
        val deletePath = "images/note_1_img_200.jpg"
        storage.getImageFile(keepPath).parentFile?.mkdirs()
        storage.getImageFile(keepPath).writeText("keep")
        storage.getImageFile(deletePath).writeText("delete")

        val result = storage.deleteImage(deletePath)

        assertTrue(result)
        assertTrue(storage.getImageFile(keepPath).exists())
        assertFalse(storage.getImageFile(deletePath).exists())
    }
}
