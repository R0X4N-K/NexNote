package io.github.r0x4nk.nexnote.fileimport

import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.IntentCompat
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.insertStandaloneMarkdownBlock
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class ExternalShareImporter(
    private val contentResolver: ContentResolver,
    private val saveNote: SaveNoteUseCase,
    private val indexNoteTags: IndexNoteTagsUseCase,
    private val copyImage: CopyNoteImageToInternalUseCase,
    private val deleteImage: DeleteNoteImageUseCase,
    private val moveNoteToTrash: MoveNoteToTrashUseCase,
    private val deleteNote: DeleteNotePermanentlyUseCase,
    private val strings: StringProvider,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun importFrom(intent: Intent?): ExternalFileImportResult = withContext(Dispatchers.IO) {
        if (intent?.action != Intent.ACTION_SEND) return@withContext ExternalFileImportResult.Ignored
        runCatchingPreservingCancellation {
            val isImage = intent.type?.startsWith("image/") == true
            if (!isImage && intent.type != "text/plain") {
                return@runCatchingPreservingCancellation ExternalFileImportResult.Failed(
                    strings.get(R.string.import_error_unsupported_shared)
                )
            }
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
            val title = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString().orEmpty()
            if (text.toByteArray().size > TextFileImportParser.MAX_CONTENT_BYTES ||
                title.toByteArray().size > TextFileImportParser.MAX_CONTENT_BYTES
            ) {
                return@runCatchingPreservingCancellation ExternalFileImportResult.Failed(
                    strings.get(R.string.import_error_shared_too_large)
                )
            }
            val imageUri = if (isImage) {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: intent.clipData?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.uri
            } else null
            if (isImage && imageUri?.scheme != ContentResolver.SCHEME_CONTENT) {
                return@runCatchingPreservingCancellation ExternalFileImportResult.Failed(
                    strings.get(R.string.import_error_shared_image_unavailable)
                )
            }
            if (!isImage && text.isBlank()) {
                return@runCatchingPreservingCancellation ExternalFileImportResult.Failed(
                    strings.get(R.string.import_error_shared_text_empty)
                )
            }
            imageUri?.let { uri ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, bounds)
                }
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid shared image" }
            }
            val timestamp = nowMillis()
            persist(Note(
                title = title,
                content = text,
                isMarkdown = isImage,
                creationDate = timestamp,
                lastModifiedDate = timestamp
            ), imageUri)
        }.getOrElse { error ->
            NexNoteDebugLog.persistence("externalShareImportFailed", NexNoteDebugLog.throwableSummary(error))
            ExternalFileImportResult.Failed(strings.get(R.string.import_error_shared_import_failed))
        }
    }

    // Finish the save or rollback even if the receiving activity is closed mid-import.
    private suspend fun persist(draft: Note, imageUri: Uri?): ExternalFileImportResult =
        withContext(NonCancellable) {
            val noteId = saveNote(draft)
            var imagePath: String? = null
            val stored = try {
                if (imageUri == null) draft else {
                    val path = copyImage(noteId) { contentResolver.openInputStream(imageUri) }
                    imagePath = path
                    draft.copy(
                        id = noteId,
                        content = insertStandaloneMarkdownBlock(
                            text = draft.content,
                            block = "![${strings.get(R.string.markdown_image_alt_placeholder)}]($path)",
                            offset = draft.content.length
                        ).text,
                        imagePaths = listOf(path)
                    ).also { saveNote(it) }
                }
            } catch (error: Exception) {
                runCatching {
                    moveNoteToTrash(noteId)
                    deleteNote(noteId)
                }.onFailure(error::addSuppressed)
                imagePath?.let { path ->
                    runCatching { deleteImage(path) }.onFailure(error::addSuppressed)
                }
                throw error
            }
            runCatchingPreservingCancellation { indexNoteTags(noteId, stored.content) }
                .onFailure { error ->
                    NexNoteDebugLog.persistence("externalShareTagIndexFailed", NexNoteDebugLog.throwableSummary(error))
                }
            ExternalFileImportResult.Imported(noteId)
        }
}
