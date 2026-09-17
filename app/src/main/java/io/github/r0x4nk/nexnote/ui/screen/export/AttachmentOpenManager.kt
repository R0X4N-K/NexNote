package io.github.r0x4nk.nexnote.ui.screen.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.util.copyStreaming
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Shares a streaming snapshot, never the mutable private original or a decrypted Vault payload. */
internal class AttachmentOpenManager(private val context: Context) {
    suspend fun buildIntent(attachment: NoteAttachment, provider: (String) -> File): Intent {
        var copy: File? = null
        try {
            return withContext(Dispatchers.IO) {
                require(NoteAttachment.isAttachmentPath(attachment.path))
                val source = provider(attachment.path)
                val target = ExportCache(context.cacheDir).prepareFile("attachment.${attachment.extension}")
                copy = target
                source.inputStream().buffered().use { input ->
                    input.mark(64)
                    val header = ByteArray(64)
                    val count = input.read(header)
                    if (count > 0 && VaultEncryptedFile.isEncoded(header.copyOf(count))) {
                        throw IOException("Vault attachments cannot be opened externally")
                    }
                    input.reset()
                    target.outputStream().use { output -> copyStreaming(input, output, coroutineContext::ensureActive) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(attachment.extension)
                        ?: "application/octet-stream")
                    clipData = ClipData.newUri(context.contentResolver, attachment.displayName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { copy?.delete() }
            throw error
        }
    }
}
