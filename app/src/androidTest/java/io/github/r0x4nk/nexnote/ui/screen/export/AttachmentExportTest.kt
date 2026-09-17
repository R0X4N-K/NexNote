package io.github.r0x4nk.nexnote.ui.screen.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.util.AttachmentMarkdown
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentExportTest {
    private val attachment = NoteAttachment("images/attachments/note_1_abcd.pdf", "Report.pdf")

    private companion object {
        const val IMAGE_PATH = "images/photo.jpg"
    }

    @Test fun everyShareFormatCarriesOriginalAttachmentAndReadOnlyGrant() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("attachment-test-", ".pdf", context.cacheDir).apply { writeText("%PDF-original") }
        try {
            val manager = ExportManager(context, { source })
            val note = Note(title = "Document", content = AttachmentMarkdown.format(attachment), imagePaths = listOf(attachment.path))
            for (format in listOf(ExportFormat.MD, ExportFormat.TXT, ExportFormat.PDF)) {
                val intent = manager.buildShareIntent(listOf(note), format)
                assertEquals("application/zip", intent.type)
                assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val uri = requireNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
                assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
                val entries = linkedMapOf<String, ByteArray>()
                ZipInputStream(context.contentResolver.openInputStream(uri)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries[entry.name] = zip.readBytes()
                    }
                }
                assertEquals(2, entries.size)
                assertArrayEquals(source.readBytes(), entries.getValue("attachments/1_Report.pdf"))
            }
        } finally { source.delete() }
    }

    @Test fun excludingMediaProducesDirectDocumentsWithoutReadingMedia() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ExportManager(context, { error("Media must not be opened") })
        val note = Note(title = "Text only", content = "Hello\n![Photo](images/photo.jpg)\n" + AttachmentMarkdown.format(attachment),
            imagePaths = listOf("images/photo.jpg", attachment.path))
        for ((format, mime) in listOf(ExportFormat.TXT to "text/plain", ExportFormat.MD to "text/markdown")) {
            val intent = manager.buildShareIntent(listOf(note), format, includeMedia = false)
            assertEquals(mime, intent.type)
            val uri = requireNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertFalse(bytes.take(2) == listOf(80.toByte(), 75.toByte()))
            assertTrue(bytes.toString(Charsets.UTF_8).contains("Hello"))
            assertFalse(bytes.toString(Charsets.UTF_8).contains("images/"))
        }
    }

    @Test fun pdfEmbedsImagesEvenWhenTheZipArchiveIsDisabled() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = File.createTempFile("pdf-image-", ".png", context.cacheDir).apply {
            java.io.FileOutputStream(this).use { output ->
                android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            }
        }
        val openedPaths = mutableListOf<String>()
        try {
            val manager = ExportManager(context, { path ->
                openedPaths += path
                image
            })
            val note = Note(
                title = "With image",
                content = "Hello\n![image]($IMAGE_PATH)",
                imagePaths = listOf(IMAGE_PATH)
            )

            val intent = manager.buildShareIntent(listOf(note), ExportFormat.PDF, includeMedia = false)
            assertEquals("application/pdf", intent.type)
            val uri = requireNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }

            assertEquals(listOf(IMAGE_PATH), openedPaths)
            assertTrue(String(bytes, 0, minOf(4, bytes.size), Charsets.US_ASCII).startsWith("%PDF"))
        } finally {
            image.delete()
        }
    }

    @Test fun openingUsesSnapshotWithPdfMimeAndNeverExposesOriginal() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("attachment-test-", ".pdf", context.cacheDir).apply { writeText("original") }
        try {
            val intent = AttachmentOpenManager(context).buildIntent(attachment) { source }
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("application/pdf", intent.type)
            assertEquals("content", intent.data?.scheme)
            source.writeText("changed")
            val snapshot = context.contentResolver.openInputStream(requireNotNull(intent.data))!!.bufferedReader().use { it.readText() }
            assertEquals("original", snapshot)
        } finally { source.delete() }
    }
}
