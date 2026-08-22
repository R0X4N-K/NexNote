package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.InputStream

class DuplicateNoteUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `duplicate creates a new active note and indexes its tags`() = runTest {
        val noteRepository = FakeDuplicateNoteRepository(nextId = 100L)
        val tagRepository = FakeDuplicateTagRepository()
        val useCase = DuplicateNoteUseCase(
            noteRepository = noteRepository,
            tagRepository = tagRepository,
            imageStorage = FakeDuplicateImageStorage(tempFolder.root)
        )
        val source = Note(
            id = 7L,
            title = "Plan",
            content = "Ship #release",
            isMarkdown = true,
            creationDate = 123L,
            timezone = "Europe/Rome",
            isDeleted = true,
            deletedDate = 456L,
            isPinned = true,
            backgroundColor = 0xFF112233.toInt(),
            isPreviewMode = true
        )

        val duplicateId = useCase(source)

        val duplicate = noteRepository.savedNotes.getValue(duplicateId)
        assertEquals(100L, duplicateId)
        assertEquals("Plan", duplicate.title)
        assertEquals("Ship #release", duplicate.content)
        assertEquals(123L, duplicate.creationDate)
        assertEquals("Europe/Rome", duplicate.timezone)
        assertTrue(duplicate.isPinned)
        assertTrue(duplicate.isPreviewMode)
        assertEquals(0xFF112233.toInt(), duplicate.backgroundColor)
        assertFalse(duplicate.isDeleted)
        assertNull(duplicate.deletedDate)
        assertEquals(100L to "Ship #release", tagRepository.indexedNotes.single())
    }

    @Test
    fun `duplicate copies image files and rewrites markdown image paths`() = runTest {
        val noteRepository = FakeDuplicateNoteRepository(nextId = 200L)
        val tagRepository = FakeDuplicateTagRepository()
        val imageStorage = FakeDuplicateImageStorage(tempFolder.root)
        val sourcePath = "images/note_1_img_10.jpg"
        imageStorage.getImageFile(sourcePath).parentFile?.mkdirs()
        imageStorage.getImageFile(sourcePath).writeText("image bytes")
        val useCase = DuplicateNoteUseCase(noteRepository, tagRepository, imageStorage)
        val source = Note(
            id = 1L,
            content = "Before\n![image]($sourcePath)\nAfter",
            imagePaths = listOf(sourcePath)
        )

        val duplicateId = useCase(source)

        val duplicate = noteRepository.savedNotes.getValue(duplicateId)
        val duplicatePath = "images/note_200_img_0.jpg"
        assertEquals("Before\n![image]($duplicatePath)\nAfter", duplicate.content)
        assertEquals(listOf(duplicatePath), duplicate.imagePaths)
        assertEquals("image bytes", imageStorage.getImageFile(duplicatePath).readText())
        assertEquals(2, noteRepository.saveCount)
    }

    @Test
    fun `duplicate rejects Vault notes before saving or indexing`() = runTest {
        val noteRepository = FakeDuplicateNoteRepository(nextId = 300L)
        val tagRepository = FakeDuplicateTagRepository()
        val useCase = DuplicateNoteUseCase(
            noteRepository = noteRepository,
            tagRepository = tagRepository,
            imageStorage = FakeDuplicateImageStorage(tempFolder.root)
        )

        try {
            useCase(Note(id = 9L, title = "Secret", content = "Private", isInVault = true))
            fail("Expected Vault notes to be rejected by the normal duplicate path.")
        } catch (_: IllegalArgumentException) {
            // Expected: Vault notes need a dedicated encrypted duplication flow.
        }

        assertTrue(noteRepository.savedNotes.isEmpty())
        assertEquals(0, noteRepository.saveCount)
        assertTrue(tagRepository.indexedNotes.isEmpty())
    }
}

private class FakeDuplicateNoteRepository(
    private var nextId: Long
) : NoteRepository {
    val savedNotes = linkedMapOf<Long, Note>()
    var saveCount = 0

    override val allNotes: Flow<List<Note>> = flowOf(emptyList())
    override val allNotesSortedAsc: Flow<List<Note>> = flowOf(emptyList())
    override val deletedNotes: Flow<List<Note>> = flowOf(emptyList())
    override val noteLinkCandidates: Flow<List<NoteLinkCandidate>> = flowOf(emptyList())
    override val distinctActiveDays: Flow<Set<Long>> = flowOf(emptySet())
    override val distinctLocalDays: Flow<Set<Long>> = flowOf(emptySet())

    override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
    override fun searchNotesScored(query: String): Flow<List<ScoredNote>> = flowOf(emptyList())
    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>> =
        flowOf(emptyList())

    override suspend fun getNoteById(id: Long): Note? = savedNotes[id]

    override suspend fun saveNote(note: Note): Long {
        saveCount++
        val id = if (note.id == 0L) nextId++ else note.id
        savedNotes[id] = note.copy(id = id, lastModifiedDate = 999L)
        return id
    }

    override suspend fun moveToTrash(id: Long) = Unit
    override suspend fun restoreFromTrash(id: Long) = Unit
    override suspend fun deleteNotePermanently(id: Long) = Unit
    override suspend fun emptyTrash() = Unit
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
}

private class FakeDuplicateTagRepository : TagRepository {
    val indexedNotes = mutableListOf<Pair<Long, String>>()

    override fun getAllTagsByUsageDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByUsageAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun searchTags(query: String): Flow<List<Tag>> = flowOf(emptyList())
    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> = flowOf(emptyList())
    override fun getMostUsedTags(limit: Int): Flow<List<Tag>> = flowOf(emptyList())
    override fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>> = flowOf(emptySet())
    override fun observeNotesForTag(tagName: String): Flow<List<Note>> = flowOf(emptyList())

    override suspend fun indexNoteTags(noteId: Long, content: String) {
        indexedNotes += noteId to content
    }

    override suspend fun deleteTag(tagName: String) = Unit
}

private class FakeDuplicateImageStorage(
    private val filesDir: File
) : NoteImageStorage {
    private var copyIndex = 0

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String {
        val relativePath = "images/note_${noteId}_img_${copyIndex++}.jpg"
        val destination = getImageFile(relativePath)
        destination.parentFile?.mkdirs()
        val input = openInputStream() ?: throw IOException("Missing source")
        input.use { source ->
            destination.outputStream().use { output -> source.copyTo(output) }
        }
        return relativePath
    }

    override suspend fun deleteImage(relativePath: String): Boolean = true

    override fun getImageFile(relativePath: String): File =
        File(filesDir, relativePath)
}
