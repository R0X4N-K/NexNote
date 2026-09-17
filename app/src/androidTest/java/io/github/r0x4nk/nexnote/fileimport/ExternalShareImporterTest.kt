package io.github.r0x4nk.nexnote.fileimport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.local.InternalNoteImageStorage
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TagRepositoryImpl
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalShareImporterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: NexNoteDatabase
    private lateinit var root: File
    private lateinit var storage: InternalNoteImageStorage
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java).build()
        root = File(context.cacheDir, "exports/share-tests").apply { mkdirs() }
        storage = InternalNoteImageStorage(root)
        repository = NoteRepositoryImpl(database.noteDao(), storage)
    }

    @After
    fun tearDown() {
        database.close()
        root.deleteRecursively()
    }

    @Test
    fun sharedTextPreservesSubjectBodyAndIndexesTags() = runTest {
        val text = "https://example.com/\nBody with #shared"
        val result = importer().importFrom(textIntent(text).putExtra(Intent.EXTRA_SUBJECT, "Page title"))
        val note = stored(result)
        assertEquals("Page title", note.title)
        assertEquals(text, note.content)
        assertFalse(note.isMarkdown)
        assertEquals(1234L, note.creationDate)
        assertEquals(listOf("shared"), TagRepositoryImpl(database, database.tagDao(), database.noteContentPatchDao()).getTagsForNote(note.id).first().map { it.name })
    }

    @Test
    fun imageAndCaptionAreCopiedAndRemainReadableAfterSourceDeletion() = runTest {
        val source = imageFile()
        val note = stored(importer().importFrom(imageIntent(source).putExtra(Intent.EXTRA_TEXT, "Caption")))
        source.delete()
        assertEquals(1, note.imagePaths.size)
        assertTrue(storage.getImageFile(note.imagePaths.single()).length() > 0)
        assertTrue(note.content.startsWith("Caption"))
        assertTrue(note.content.contains("![image](${note.imagePaths.single()})"))
        assertTrue(note.isMarkdown)
    }

    @Test
    fun imageCanBeReadFromSingleItemClipData() = runTest {
        val intent = Intent(Intent.ACTION_SEND).setType("image/png").apply {
            clipData = ClipData.newUri(context.contentResolver, "image", uri(imageFile()))
        }
        assertEquals(1, stored(importer().importFrom(intent)).imagePaths.size)
    }

    @Test
    fun invalidSharesDoNotCreateNotes() = runTest {
        val cases = listOf(
            textIntent(" "),
            textIntent("x".repeat(TextFileImportParser.MAX_CONTENT_BYTES + 1)),
            Intent(Intent.ACTION_SEND).setType("application/pdf"),
            Intent(Intent.ACTION_SEND).setType("image/png"),
            Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///private.png")),
            Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, Uri.parse("https://example.com/a.png"))
        )
        for (intent in cases) assertTrue(importer().importFrom(intent) is ExternalFileImportResult.Failed)
        assertTrue(repository.allNotes.first().isEmpty())
        assertTrue(repository.deletedNotes.first().isEmpty())
    }

    @Test
    fun corruptOrUnavailableImageRollsBackDraft() = runTest {
        val corrupt = File(root, "corrupt.png").apply { writeText("not an image") }
        assertTrue(importer().importFrom(imageIntent(corrupt)) is ExternalFileImportResult.Failed)
        val missing = File(root, "missing.png")
        assertTrue(importer().importFrom(imageIntent(missing)) is ExternalFileImportResult.Failed)
        assertTrue(repository.allNotes.first().isEmpty())
        assertTrue(repository.deletedNotes.first().isEmpty())
        assertTrue(File(root, "images").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun failedImageNoteUpdateRemovesDraftAndCopiedImage() = runTest {
        val failingRepository = object : NoteRepository by repository {
            override suspend fun saveNote(note: Note): Long {
                if (note.imagePaths.isNotEmpty()) throw IOException("save failed")
                return repository.saveNote(note)
            }
        }
        assertTrue(importer(failingRepository).importFrom(imageIntent(imageFile())) is ExternalFileImportResult.Failed)
        assertTrue(repository.allNotes.first().isEmpty())
        assertTrue(repository.deletedNotes.first().isEmpty())
        assertTrue(File(root, "images").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun unrelatedActionsAreIgnored() = runTest {
        assertEquals(ExternalFileImportResult.Ignored, importer().importFrom(null))
        assertEquals(ExternalFileImportResult.Ignored, importer().importFrom(textIntent("body").setAction(Intent.ACTION_SEND_MULTIPLE)))
        assertTrue(repository.allNotes.first().isEmpty())
        assertTrue(repository.deletedNotes.first().isEmpty())
    }

    private fun importer(notes: NoteRepository = repository) = ExternalShareImporter(
        context.contentResolver, SaveNoteUseCase(notes),
        IndexNoteTagsUseCase(TagRepositoryImpl(database, database.tagDao(), database.noteContentPatchDao())),
        CopyNoteImageToInternalUseCase(storage), DeleteNoteImageUseCase(storage),
        MoveNoteToTrashUseCase(notes), DeleteNotePermanentlyUseCase(notes),
        strings = StringProvider { id, args -> context.getString(id, *args) },
        nowMillis = { 1234L }
    )

    private suspend fun stored(result: ExternalFileImportResult): Note {
        assertTrue("Expected imported note, got $result", result is ExternalFileImportResult.Imported)
        return requireNotNull(repository.getNoteById((result as ExternalFileImportResult.Imported).noteId))
    }

    private fun textIntent(text: String) = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    private fun imageIntent(file: File) = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri(file))
    private fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    private fun imageFile() = File(root, "shared.png").also { file ->
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
}
