package com.example.nexnote.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
    var imageBitmap by remember(file.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    var loadFailed by remember(file.absolutePath) { mutableStateOf(false) }

    LaunchedEffect(file.absolutePath) {
        imageBitmap = null
        loadFailed = false
        val result = loadMarkdownBitmap(file)
        if (result != null) imageBitmap = result else loadFailed = true
    }

    MarkdownImageContent(imageBitmap, loadFailed, altText)
}

@Composable
private fun MarkdownImagePlaceholder(altText: String) {
    Text(
        text     = "\uD83D\uDCF7 ${altText.ifEmpty { "Immagine" }}",
        style    = MaterialTheme.typography.bodyMedium,
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private suspend fun loadMarkdownBitmap(file: File): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
    }
}

@Composable
private fun MarkdownImageContent(
    imageBitmap: ImageBitmap?,
    loadFailed: Boolean,
    altText: String
) {
    when {
        imageBitmap != null -> LoadedMarkdownImage(imageBitmap, altText)
        loadFailed -> MissingMarkdownImage()
        else -> LoadingMarkdownImage()
    }
}

@Composable
private fun LoadedMarkdownImage(
    imageBitmap: ImageBitmap?,
    altText: String
) {
    Image(
        bitmap             = imageBitmap!!,
        contentDescription = altText.ifEmpty { "Image in note" },
        modifier           = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
private fun LoadingMarkdownImage() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}
