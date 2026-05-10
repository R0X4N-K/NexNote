package io.github.r0x4nk.nexnote.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.ImageFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── Data models ─────────────────────────────────────────────────────────

private data class MarkdownImageSize(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float = width.toFloat() / height.coerceAtLeast(1).toFloat()
}

private data class MarkdownImageLoadResult(
    val bitmap: ImageBitmap,
    val size: MarkdownImageSize
)

// ── Public composable ───────────────────────────────────────────────────

/**
 * Loads and renders a local image asynchronously.
 *
 * - `imageFileProvider == null` -> text placeholder
 * - File missing / decode error -> broken-image icon
 * - Otherwise -> full-width image with rounded corners
 */
@Composable
internal fun MarkdownImageBlock(
    imageFileProvider: ((String) -> File)?,
    relativePath: String,
    altText: String
) {
    if (imageFileProvider == null) {
        MarkdownImagePlaceholder(altText)
        return
    }

    val file = remember(imageFileProvider, relativePath) { imageFileProvider(relativePath) }
    val initialImageSize = remember(file.absolutePath, file.lastModified()) {
        readMarkdownImageSize(file)
    }
    var imageBitmap by remember(file.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    var imageSize by remember(file.absolutePath) { mutableStateOf(initialImageSize) }
    var loadFailed by remember(file.absolutePath) { mutableStateOf(false) }

    LaunchedEffect(file.absolutePath, initialImageSize) {
        imageBitmap = null
        imageSize = initialImageSize
        loadFailed = false

        val result = loadMarkdownBitmap(file, initialImageSize)
        if (result != null) {
            imageSize = result.size
            imageBitmap = result.bitmap
        } else {
            loadFailed = true
        }
    }

    MarkdownImageContent(imageBitmap, imageSize, loadFailed, altText)
}

// ── Placeholder / error / loading states ────────────────────────────────

@Composable
private fun MarkdownImagePlaceholder(altText: String) {
    Text(
        text     = "📷 ${altText.ifEmpty { "Immagine" }}",
        style    = MaterialTheme.typography.bodyMedium,
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MarkdownImageContent(
    imageBitmap: ImageBitmap?,
    imageSize: MarkdownImageSize?,
    loadFailed: Boolean,
    altText: String
) {
    when {
        imageBitmap != null -> LoadedMarkdownImage(imageBitmap, imageSize, altText)
        loadFailed -> MissingMarkdownImage()
        else -> LoadingMarkdownImage(imageSize)
    }
}

@Composable
private fun LoadedMarkdownImage(
    imageBitmap: ImageBitmap,
    imageSize: MarkdownImageSize?,
    altText: String
) {
    Image(
        bitmap             = imageBitmap,
        contentDescription = altText.ifEmpty { "Image in note" },
        modifier           = Modifier
            .markdownImageFrame(imageSize ?: imageBitmap.intrinsicMarkdownSize())
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun MissingMarkdownImage() {
    Row(
        modifier              = Modifier.padding(vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.BrokenImage,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            modifier           = Modifier.size(20.dp)
        )
        Text(
            text  = "Immagine non trovata",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun LoadingMarkdownImage(imageSize: MarkdownImageSize?) {
    Box(
        modifier         = Modifier
            .markdownImageFrame(imageSize),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

// ── Bitmap loading ──────────────────────────────────────────────────────

/**
 * Reads the raw pixel dimensions without allocating a full bitmap.
 * Called synchronously from `remember` to provide an immediate aspect ratio
 * for the placeholder frame.
 */
private fun readMarkdownImageSize(file: File): MarkdownImageSize? {
    if (!file.exists()) return null

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return markdownImageSizeOrNull(options.outWidth, options.outHeight)
}

/**
 * Decodes the image bitmap with safe downsampling to prevent Canvas crashes
 * on oversized images.
 *
 * Reuses [knownSize] (from the earlier [readMarkdownImageSize] call) to
 * compute the [BitmapFactory.Options.inSampleSize] without a redundant
 * bounds-only decode pass.
 */
private suspend fun loadMarkdownBitmap(
    file: File,
    knownSize: MarkdownImageSize?
): MarkdownImageLoadResult? {
    return withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        val sampleSize = knownSize?.let { size ->
            val longestSide = maxOf(size.width, size.height)
            ImageFileManager.calculateSampleSize(longestSide)
        } ?: 1

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: return@withContext null

        MarkdownImageLoadResult(
            bitmap = bitmap.asImageBitmap(),
            size = MarkdownImageSize(width = bitmap.width, height = bitmap.height)
        )
    }
}

// ── Layout helpers ──────────────────────────────────────────────────────

private fun Modifier.markdownImageFrame(imageSize: MarkdownImageSize?): Modifier {
    val base = fillMaxWidth().padding(vertical = 8.dp)
    return if (imageSize != null) {
        base.aspectRatio(imageSize.aspectRatio)
    } else {
        base.height(100.dp)
    }
}

private fun ImageBitmap.intrinsicMarkdownSize(): MarkdownImageSize =
    MarkdownImageSize(width = width, height = height)

private fun markdownImageSizeOrNull(width: Int, height: Int): MarkdownImageSize? {
    return if (width > 0 && height > 0) MarkdownImageSize(width, height) else null
}
