package com.example.nexnote.util

import java.io.File

/**
 * Manages note image files stored in internal storage.
 *
 * Images live under `[filesDir]/images/note_{noteId}_img_{timestamp}.jpg`.
 * All paths stored in the database are relative to [filesDir] so they stay
 * valid even if the app is reinstalled to a different data partition.
 */
object ImageFileManager {

    const val IMAGES_DIR = "images"

    /**
     * Builds a deterministic relative path for an image belonging to [noteId],
     * disambiguated by [timestamp] (typically [System.currentTimeMillis]).
     *
     * Format: `images/note_{noteId}_img_{timestamp}.jpg`
     */
    fun buildRelativePath(noteId: Long, timestamp: Long): String =
        "$IMAGES_DIR/note_${noteId}_img_${timestamp}.jpg"

    /**
     * Returns the absolute [File] for the given [relativePath] under [filesDir].
     * The file may or may not exist — callers must check before reading.
     */
    fun getImageFile(filesDir: File, relativePath: String): File =
        File(filesDir, relativePath)

    /**
     * Creates the shared images directory under [filesDir] if it does not already
     * exist and returns it. Safe to call repeatedly (idempotent).
     */
    fun ensureImageDir(filesDir: File): File =
        File(filesDir, IMAGES_DIR).also { it.mkdirs() }

    /**
     * Deletes the image at [relativePath] under [filesDir].
     *
     * Returns `true` if the file was deleted or did not exist;
     * `false` if the file exists but could not be deleted.
     */
    fun deleteImage(filesDir: File, relativePath: String): Boolean {
        val file = getImageFile(filesDir, relativePath)
        return if (file.exists()) file.delete() else true
    }
}
