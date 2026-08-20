package io.github.r0x4nk.nexnote.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.max

/**
 * Processes images imported from external sources before persisting them as
 * note attachments.
 *
 * Two transformations are applied when necessary:
 *
 * 1. **EXIF orientation correction** — Many camera apps write photos with a
 *    default pixel orientation (typically landscape) and embed an EXIF tag that
 *    viewers should use to rotate the image. [BitmapFactory] does not apply that
 *    tag automatically, so without correction images may appear rotated. This
 *    processor physically rotates/flips the pixels so the stored file is always
 *    upright regardless of the viewer.
 *
 * 2. **Downsampling** — High-resolution camera images (often 8000 px+) can
 *    exceed Android's hardware-texture limit and crash the Canvas renderer.
 *    Images whose longest side exceeds [ImageFileManager.MAX_IMAGE_DIMENSION_PX]
 *    are downscaled during import to prevent out-of-memory errors and Canvas
 *    crashes while retaining more than enough detail for in-app display.
 *
 * When neither transformation is required the raw bytes are copied directly,
 * avoiding unnecessary re-encoding and quality loss.
 *
 * Usage:
 * ```
 * NoteImageProcessor.processAndSave(
 *     inputStreamProvider = { contentResolver.openInputStream(uri) },
 *     destination = targetFile
 * )
 * ```
 */
object NoteImageProcessor {

    private const val JPEG_QUALITY = 90
    const val MAX_IMAGE_BYTES = 25L * 1024L * 1024L

    /**
     * Copies an image from [inputStreamProvider] to [destination], applying EXIF
     * orientation correction and downsampling when necessary.
     *
     * If the image is already correctly oriented **and** fits within the
     * configured maximum dimension, the raw bytes are copied directly to avoid
     * unnecessary re-encoding.
     *
     * @param inputStreamProvider factory that opens a fresh [InputStream] each
     *   time it is called. It may be invoked up to three times (bounds check,
     *   EXIF read, bitmap decode).
     * @param destination the file to write the processed image into.
     * @throws IOException if the stream cannot be read or the image cannot be
     *   decoded.
     */
    fun processAndSave(
        inputStreamProvider: () -> InputStream?,
        destination: File
    ) {
        val dimensions = readImageDimensions(inputStreamProvider)
        val orientation = readExifOrientation(inputStreamProvider)
        val needsRotation = requiresTransformation(orientation)
        val needsDownscale = dimensions != null && exceedsMaxDimension(dimensions)

        if (!needsRotation && !needsDownscale) {
            copyRawBytes(inputStreamProvider, destination)
            return
        }

        val sampleSize = if (dimensions != null) {
            ImageFileManager.calculateSampleSize(dimensions.longestSide)
        } else {
            1
        }

        decodeTransformAndSave(inputStreamProvider, destination, orientation, sampleSize)
    }

    // ── Dimension reading ───────────────────────────────────────────────

    private data class ImageDimensions(val width: Int, val height: Int) {
        val longestSide: Int get() = max(width, height)
    }

    /**
     * Reads the raw pixel dimensions without allocating a full bitmap.
     * Returns `null` when the stream cannot be opened or the format is not
     * recognized.
     */
    private fun readImageDimensions(inputStreamProvider: () -> InputStream?): ImageDimensions? {
        val stream = inputStreamProvider() ?: return null
        return stream.use { input ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                ImageDimensions(options.outWidth, options.outHeight)
            } else {
                null
            }
        }
    }

    private fun exceedsMaxDimension(dimensions: ImageDimensions): Boolean =
        dimensions.longestSide > ImageFileManager.MAX_IMAGE_DIMENSION_PX

    // ── EXIF reading ────────────────────────────────────────────────────

    /**
     * Reads the EXIF orientation tag from the image stream.
     * Returns [ExifInterface.ORIENTATION_NORMAL] when the tag is absent or
     * the stream cannot be opened.
     */
    private fun readExifOrientation(inputStreamProvider: () -> InputStream?): Int {
        val stream = inputStreamProvider() ?: return ExifInterface.ORIENTATION_NORMAL
        return stream.use { input ->
            try {
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: IOException) {
                ExifInterface.ORIENTATION_NORMAL
            }
        }
    }

    // ── Transformation logic ────────────────────────────────────────────

    /**
     * Returns `true` when the EXIF [orientation] requires a rotation or flip.
     */
    private fun requiresTransformation(orientation: Int): Boolean =
        orientation != ExifInterface.ORIENTATION_NORMAL &&
            orientation != ExifInterface.ORIENTATION_UNDEFINED

    /**
     * Builds the [Matrix] that maps the original pixel coordinates to the
     * corrected orientation.
     *
     * Covers all eight EXIF orientation values defined by the standard.
     */
    private fun buildTransformMatrix(orientation: Int, width: Int, height: Int): Matrix =
        Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                    setScale(-1f, 1f, width / 2f, height / 2f)

                ExifInterface.ORIENTATION_ROTATE_180 ->
                    setRotate(180f, width / 2f, height / 2f)

                ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                    setScale(1f, -1f, width / 2f, height / 2f)

                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f, width / 2f, height / 2f)
                    postScale(-1f, 1f, width / 2f, height / 2f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 ->
                    setRotate(90f, width / 2f, height / 2f)

                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f, width / 2f, height / 2f)
                    postScale(-1f, 1f, width / 2f, height / 2f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 ->
                    setRotate(-90f, width / 2f, height / 2f)
            }
        }

    // ── I/O helpers ─────────────────────────────────────────────────────

    /**
     * Copies the raw bytes from the input stream to [destination] without any
     * transformation. Used when the image is already correctly oriented and
     * within the size limit.
     */
    private fun copyRawBytes(inputStreamProvider: () -> InputStream?, destination: File) {
        val stream = inputStreamProvider()
            ?: throw IOException("Image input stream is unavailable")
        stream.use { input ->
            destination.outputStream().use { output ->
                copyBounded(input, output, MAX_IMAGE_BYTES)
            }
        }
    }

    /**
     * Decodes the image (with optional sub-sampling), applies the EXIF
     * rotation/flip, and writes the result as JPEG.
     */
    private fun decodeTransformAndSave(
        inputStreamProvider: () -> InputStream?,
        destination: File,
        orientation: Int,
        sampleSize: Int
    ) {
        val stream = inputStreamProvider()
            ?: throw IOException("Image input stream is unavailable")

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = stream.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IOException("Failed to decode image for orientation correction")

        val rotated = if (requiresTransformation(orientation)) {
            val matrix = buildTransformMatrix(orientation, decoded.width, decoded.height)
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }

        try {
            destination.outputStream().use { output ->
                rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
        } finally {
            if (rotated !== decoded) rotated.recycle()
            decoded.recycle()
        }
    }
}
