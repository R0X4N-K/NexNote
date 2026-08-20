package io.github.r0x4nk.nexnote.util

import java.io.File
import java.io.IOException

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
     * Maximum pixel length for the longest side of a note image.
     *
     * Used both at import time (to downsample oversized camera photos) and at
     * display time (as a safety net for images stored before downsampling was
     * introduced). 2048 px keeps bitmaps under ~16 MB in ARGB_8888, well
     * within Android's Canvas texture limit while remaining sharp on
     * high-density screens.
     */
    const val MAX_IMAGE_DIMENSION_PX = 2048

    /**
     * Computes the largest power-of-two sample size that brings the
     * [longestSide] at or below [maxDimension].
     *
     * This follows the standard approach for
     * [android.graphics.BitmapFactory.Options.inSampleSize]: values are always
     * powers of two, and the decoder rounds non-power-of-two values down
     * anyway.
     *
     * @return 1 when no downsampling is needed.
     */
    fun calculateSampleSize(longestSide: Int, maxDimension: Int = MAX_IMAGE_DIMENSION_PX): Int {
        var sampleSize = 1
        while (longestSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

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
    fun getImageFile(filesDir: File, relativePath: String): File {
        val normalized = relativePath.replace('\\', '/')
        val segments = normalized.split('/')
        require(
            segments.size >= 2 &&
                segments.first() == IMAGES_DIR &&
                segments.none { it.isBlank() || it == "." || it == ".." }
        ) { "Image path is outside the image namespace" }
        require(!File(relativePath).isAbsolute) { "Absolute image paths are not allowed" }

        val canonicalFilesDir = filesDir.canonicalFile
        val logicalImageRoot = File(canonicalFilesDir, IMAGES_DIR).absoluteFile.toPath().normalize()
        val canonicalImageRoot = logicalImageRoot.toFile().canonicalFile.toPath()
        if (canonicalImageRoot != logicalImageRoot) {
            throw IOException("Image directory resolves through a filesystem link")
        }

        val candidate = File(canonicalFilesDir, normalized).canonicalFile.toPath()
        require(candidate != canonicalImageRoot && candidate.startsWith(canonicalImageRoot)) {
            "Image path escapes the image directory"
        }
        return candidate.toFile()
    }

    /**
     * Creates the shared images directory under [filesDir] if it does not already
     * exist and returns it. Safe to call repeatedly (idempotent).
     */
    fun ensureImageDir(filesDir: File): File =
        File(filesDir.canonicalFile, IMAGES_DIR).also { directory ->
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Could not create image directory")
            }
            if (!directory.isDirectory || directory.canonicalFile != directory.absoluteFile) {
                throw IOException("Image directory is not a confined directory")
            }
        }

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
