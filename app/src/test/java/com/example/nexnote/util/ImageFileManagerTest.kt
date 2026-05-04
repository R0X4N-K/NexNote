package com.example.nexnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [ImageFileManager]: pure functions with no Android dependency.
 *
 * Stream copying lives in InternalNoteImageStorage. This suite covers:
 * - relative path generation
 * - absolute path resolution
 * - image directory creation
 * - image file deletion
 */
class ImageFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File

    @Before
    fun setup() {
        filesDir = tempFolder.root
    }

    // ── buildRelativePath ────────────────────────────────────────────────────

    @Test
    fun `buildRelativePath contains noteId and timestamp`() {
        val path = ImageFileManager.buildRelativePath(noteId = 42, timestamp = 1234567890L)
        assertEquals("images/note_42_img_1234567890.jpg", path)
    }

    @Test
    fun `buildRelativePath with zero noteId`() {
        val path = ImageFileManager.buildRelativePath(noteId = 0, timestamp = 999L)
        assertEquals("images/note_0_img_999.jpg", path)
    }

    @Test
    fun `buildRelativePath uses images directory prefix`() {
        val path = ImageFileManager.buildRelativePath(noteId = 1, timestamp = 1L)
        assertTrue(path.startsWith(ImageFileManager.IMAGES_DIR + "/"))
    }

    @Test
    fun `buildRelativePath uses jpg extension`() {
        val path = ImageFileManager.buildRelativePath(noteId = 1, timestamp = 1L)
        assertTrue(path.endsWith(".jpg"))
    }

    // ── getImageFile ─────────────────────────────────────────────────────────

    @Test
    fun `getImageFile returns correct absolute path`() {
        val file = ImageFileManager.getImageFile(filesDir, "images/note_1_img_123.jpg")
        assertEquals(
            File(filesDir, "images/note_1_img_123.jpg").absolutePath,
            file.absolutePath
        )
    }

    @Test
    fun `getImageFile with nested path`() {
        val file = ImageFileManager.getImageFile(filesDir, "images/note_99_img_0.jpg")
        assertTrue(file.absolutePath.contains("images"))
        assertTrue(file.absolutePath.endsWith("note_99_img_0.jpg"))
    }

    // ── ensureImageDir ───────────────────────────────────────────────────────

    @Test
    fun `ensureImageDir creates directory if not exists`() {
        val dir = ImageFileManager.ensureImageDir(filesDir)
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertEquals(ImageFileManager.IMAGES_DIR, dir.name)
    }

    @Test
    fun `ensureImageDir is idempotent`() {
        ImageFileManager.ensureImageDir(filesDir)
        val dir = ImageFileManager.ensureImageDir(filesDir)
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun `ensureImageDir returns child of filesDir`() {
        val dir = ImageFileManager.ensureImageDir(filesDir)
        assertEquals(filesDir.absolutePath, dir.parentFile?.absolutePath)
    }

    // ── deleteImage ──────────────────────────────────────────────────────────

    @Test
    fun `deleteImage removes existing file`() {
        val dir = ImageFileManager.ensureImageDir(filesDir)
        val file = File(dir, "note_1_img_123.jpg")
        file.writeText("fake image data")
        assertTrue(file.exists())

        val result = ImageFileManager.deleteImage(filesDir, "images/note_1_img_123.jpg")
        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun `deleteImage returns true for non-existent file`() {
        val result = ImageFileManager.deleteImage(filesDir, "images/non_existent.jpg")
        assertTrue(result)
    }

    @Test
    fun `deleteImage does not affect other files in directory`() {
        val dir = ImageFileManager.ensureImageDir(filesDir)
        val keep = File(dir, "note_1_img_100.jpg").apply { writeText("keep") }
        val remove = File(dir, "note_1_img_200.jpg").apply { writeText("remove") }

        ImageFileManager.deleteImage(filesDir, "images/note_1_img_200.jpg")
        assertTrue(keep.exists())
        assertFalse(remove.exists())
    }
}
