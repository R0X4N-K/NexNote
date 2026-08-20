package io.github.r0x4nk.nexnote.fileimport

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalFileImporterTest {

    @Test
    fun contentUriIsParsedSavedIndexedAndReturnedForOpening() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val source = File(context.cacheDir, "exports/success-import.md").apply {
            parentFile?.mkdirs()
            writeText("# Imported title\n\nBody with #tag")
        }

        try {
            val noteRepository = NoteRepositoryImpl(database.noteDao(), NoOpNoteImageStorage())
            val tagRepository = RecordingTagRepository()
            val importer = ExternalFileImporter(
                contentResolver = context.contentResolver,
                saveNote = SaveNoteUseCase(noteRepository),
                indexNoteTags = IndexNoteTagsUseCase(tagRepository),
                nowMillis = { 1234L }
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                source
            )

            val result = importer.importFrom(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/markdown")
            )

            assertTrue(result is ExternalFileImportResult.Imported)
            val noteId = (result as ExternalFileImportResult.Imported).noteId
            val stored = noteRepository.getNoteById(noteId)
            assertNotNull(stored)
            assertEquals("success-import", stored?.title)
            assertEquals("# Imported title\n\nBody with #tag", stored?.content)
            assertEquals(listOf(noteId to stored?.content), tagRepository.indexed)
        } finally {
            database.close()
            source.delete()
        }
    }

    @Test
    fun fileUriIsIgnoredWithoutDereferencing() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val source = File(context.cacheDir, "must-not-open.txt").apply { writeText("secret") }
        try {
            val noteRepository = NoteRepositoryImpl(database.noteDao(), NoOpNoteImageStorage())
            val importer = ExternalFileImporter(
                context.contentResolver,
                SaveNoteUseCase(noteRepository),
                IndexNoteTagsUseCase(RecordingTagRepository())
            )

            val result = importer.importFrom(
                Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(source), "text/plain")
            )

            assertEquals(ExternalFileImportResult.Ignored, result)
            assertTrue(noteRepository.allNotes.first().isEmpty())
        } finally {
            database.close()
            source.delete()
        }
    }

    @Test
    fun unrelatedMimeIsRejectedBeforeSave() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val source = File(context.cacheDir, "exports/not-text.png").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        try {
            val noteRepository = NoteRepositoryImpl(database.noteDao(), NoOpNoteImageStorage())
            val importer = ExternalFileImporter(
                context.contentResolver,
                SaveNoteUseCase(noteRepository),
                IndexNoteTagsUseCase(RecordingTagRepository())
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                source
            )

            val result = importer.importFrom(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/png")
            )

            assertEquals(
                ExternalFileImportResult.Failed("Unsupported file type"),
                result
            )
            assertTrue(noteRepository.allNotes.first().isEmpty())
        } finally {
            database.close()
            source.delete()
        }
    }

    @Test
    fun tagIndexCancellationIsPropagatedAfterContentResolverImportAndSave() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val source = File(context.cacheDir, "exports/cancellation-import.md").apply {
            parentFile?.mkdirs()
            writeText("# Imported title\n\nBody with #tag")
        }

        try {
            val noteRepository = NoteRepositoryImpl(
                dao = database.noteDao(),
                imageStorage = NoOpNoteImageStorage()
            )
            val importer = ExternalFileImporter(
                contentResolver = context.contentResolver,
                saveNote = SaveNoteUseCase(noteRepository),
                indexNoteTags = IndexNoteTagsUseCase(CancellingTagRepository()),
                nowMillis = { 1234L }
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                source
            )
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/markdown")

            val thrown = runCatching { importer.importFrom(intent) }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            assertEquals(1, noteRepository.allNotes.first().size)
        } finally {
            database.close()
            source.delete()
        }
    }
}

private open class RecordingTagRepository : TagRepository {
    val indexed = mutableListOf<Pair<Long, String?>>()

    override fun getAllTagsByUsageDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByUsageAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun searchTags(query: String): Flow<List<Tag>> = flowOf(emptyList())
    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> = flowOf(emptyList())
    override fun getMostUsedTags(limit: Int): Flow<List<Tag>> = flowOf(emptyList())
    override fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>> = flowOf(emptySet())

    override suspend fun indexNoteTags(noteId: Long, content: String) {
        indexed += noteId to content
    }

    override suspend fun deleteTag(tagName: String) = Unit
}

private class CancellingTagRepository : RecordingTagRepository() {
    override suspend fun indexNoteTags(noteId: Long, content: String) {
        throw CancellationException("tag indexing cancelled")
    }
}
