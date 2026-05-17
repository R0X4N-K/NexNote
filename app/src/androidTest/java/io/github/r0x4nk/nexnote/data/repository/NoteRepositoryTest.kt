package io.github.r0x4nk.nexnote.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.room.Room

@RunWith(AndroidJUnit4::class)
class NoteRepositoryTest {

    private lateinit var db: NexNoteDatabase
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var imageStorage: NoOpNoteImageStorage

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        imageStorage = NoOpNoteImageStorage()
        repository = NoteRepositoryImpl(db.noteDao(), imageStorage)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Insert / Retrieve ─────────────────────────────────────────────────────

    @Test
    fun saveNote_insertNewNote_returnsPositiveId() = runTest {
        val id = repository.saveNote(Note(title = "Test", content = "Body"))
        assertTrue(id > 0)
    }

    @Test
    fun saveNote_insertAndRetrieve_matchesSavedData() = runTest {
        val id = repository.saveNote(Note(title = "Title", content = "Content"))
        val note = repository.getNoteById(id)
        assertNotNull(note)
        assertEquals("Title", note?.title)
        assertEquals("Content", note?.content)
    }

    @Test
    fun allNotes_afterInsert_containsNewNote() = runTest {
        repository.saveNote(Note(title = "Note 1"))
        repository.saveNote(Note(title = "Note 2"))
        val notes = repository.allNotes.first()
        assertEquals(2, notes.size)
    }

    @Test
    fun normalNoteSurfaces_excludeVaultNotes() = runTest {
        val normalId = repository.saveNote(
            Note(title = "Normal", content = "public alpha", creationDate = 1_000L)
        )
        val vaultId = repository.saveNote(
            Note(
                title = "Vault",
                content = "private alpha",
                creationDate = 2_000L,
                isInVault = true
            )
        )

        assertNotNull(repository.getNoteById(normalId))
        assertNull(repository.getNoteById(vaultId))
        assertEquals(listOf(normalId), repository.allNotes.first().map { it.id })
        assertEquals(listOf(normalId), repository.searchNotes("alpha").first().map { it.id })
        assertEquals(listOf(normalId), repository.noteLinkCandidates.first().map { it.id })
        assertTrue(repository.getNotesByDateRange(2_000L, 3_000L).first().isEmpty())
        assertEquals(setOf(0L), repository.distinctActiveDays.first())
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    fun saveNote_updateExistingNote_reflectsChanges() = runTest {
        val id = repository.saveNote(Note(title = "Original"))
        val note = repository.getNoteById(id)!!
        repository.saveNote(note.copy(title = "Updated"))
        val updated = repository.getNoteById(id)
        assertEquals("Updated", updated?.title)
    }

    @Test
    fun saveNote_update_setsLastModifiedDate() = runTest {
        val before = System.currentTimeMillis()
        val id = repository.saveNote(Note(title = "Test"))
        val note = repository.getNoteById(id)!!
        Thread.sleep(10)
        repository.saveNote(note.copy(title = "Updated"))
        val updated = repository.getNoteById(id)
        assertTrue((updated?.lastModifiedDate ?: 0) >= before)
    }

    // ── Soft Delete / Restore ─────────────────────────────────────────────────

    @Test
    fun moveToTrash_noteDisappearsFromAllNotes() = runTest {
        val id = repository.saveNote(Note(title = "To delete"))
        repository.moveToTrash(id)
        val active = repository.allNotes.first()
        assertTrue(active.none { it.id == id })
    }

    @Test
    fun moveToTrash_noteAppearsInDeletedNotes() = runTest {
        val id = repository.saveNote(Note(title = "In trash"))
        repository.moveToTrash(id)
        val deleted = repository.deletedNotes.first()
        assertTrue(deleted.any { it.id == id })
    }

    @Test
    fun restoreFromTrash_noteReappearsInAllNotes() = runTest {
        val id = repository.saveNote(Note(title = "Restored"))
        repository.moveToTrash(id)
        repository.restoreFromTrash(id)
        val active = repository.allNotes.first()
        assertTrue(active.any { it.id == id })
        val deleted = repository.deletedNotes.first()
        assertTrue(deleted.none { it.id == id })
    }

    // ── Permanent Delete ──────────────────────────────────────────────────────

    @Test
    fun deleteNotePermanently_removesFromDb() = runTest {
        val id = repository.saveNote(Note(title = "Permanent"))
        repository.moveToTrash(id)
        repository.deleteNotePermanently(id)
        val note = repository.getNoteById(id)
        assertNull(note)
    }

    @Test
    fun deleteNotePermanently_doesNotDeleteActiveNote() = runTest {
        // DAO filters on isDeleted=1, so an active note is never permanently deleted
        val id = repository.saveNote(Note(title = "Active"))
        repository.deleteNotePermanently(id) // no-op: note is not in trash
        val note = repository.getNoteById(id)
        assertNotNull(note)
    }

    @Test
    fun deleteNotePermanently_deletesImagesAfterDbDelete() = runTest {
        val paths = listOf("images/a.jpg", "images/b.jpg")
        val id = repository.saveNote(Note(title = "With images", imagePaths = paths))
        repository.moveToTrash(id)

        repository.deleteNotePermanently(id)

        assertNull(repository.getNoteById(id))
        assertEquals(paths, imageStorage.deletedPaths)
    }

    // ── Empty Trash ───────────────────────────────────────────────────────────

    @Test
    fun emptyTrash_removesAllDeletedNotes() = runTest {
        repository.saveNote(Note(title = "Active"))
        val id1 = repository.saveNote(Note(title = "Trash 1"))
        val id2 = repository.saveNote(Note(title = "Trash 2"))
        repository.moveToTrash(id1)
        repository.moveToTrash(id2)
        repository.emptyTrash()
        val deleted = repository.deletedNotes.first()
        assertTrue(deleted.isEmpty())
        val active = repository.allNotes.first()
        assertEquals(1, active.size)
    }

    @Test
    fun emptyTrash_deletesImagesForAllDeletedNotes() = runTest {
        repository.saveNote(Note(title = "Active", imagePaths = listOf("images/active.jpg")))
        val id1 = repository.saveNote(Note(title = "Trash 1", imagePaths = listOf("images/a.jpg")))
        val id2 = repository.saveNote(Note(title = "Trash 2", imagePaths = listOf("images/b.jpg")))
        repository.moveToTrash(id1)
        repository.moveToTrash(id2)

        repository.emptyTrash()

        assertEquals(listOf("images/a.jpg", "images/b.jpg"), imageStorage.deletedPaths)
    }

    // ── Pin ───────────────────────────────────────────────────────────────────

    @Test
    fun setPinned_true_marksNoteAsPinned() = runTest {
        val id = repository.saveNote(Note(title = "To pin"))
        repository.setPinned(id, true)
        val note = repository.getNoteById(id)
        assertTrue(note?.isPinned == true)
    }

    @Test
    fun allNotes_pinnedNotesFirst() = runTest {
        repository.saveNote(Note(title = "Regular"))
        val pinnedId = repository.saveNote(Note(title = "Pinned"))
        repository.setPinned(pinnedId, true)
        val notes = repository.allNotes.first()
        assertEquals("Pinned", notes.first().title)
    }

    // ── Image Paths ───────────────────────────────────────────────────────────

    @Test
    fun saveNote_withImagePaths_persistsAndRestores() = runTest {
        val paths = listOf("images/img1.jpg", "images/img2.jpg")
        val id = repository.saveNote(Note(title = "With images", imagePaths = paths))
        val note = repository.getNoteById(id)
        assertEquals(paths, note?.imagePaths)
    }

    @Test
    fun saveNote_emptyImagePaths_returnsEmptyList() = runTest {
        val id = repository.saveNote(Note(title = "No images", imagePaths = emptyList()))
        val note = repository.getNoteById(id)
        assertTrue(note?.imagePaths?.isEmpty() == true)
    }

    // ── Distinct Active Days ──────────────────────────────────────────────────

    @Test
    fun distinctActiveDays_returnsUTCMidnightTimestamps() = runTest {
        val dayMs = 86_400_000L
        val day1 = (System.currentTimeMillis() / dayMs) * dayMs
        val day2 = day1 + dayMs
        repository.saveNote(Note(title = "Day 1", creationDate = day1 + 3600_000L))
        repository.saveNote(Note(title = "Day 1b", creationDate = day1 + 7200_000L))
        repository.saveNote(Note(title = "Day 2", creationDate = day2 + 1000L))
        val days = repository.distinctActiveDays.first()
        assertEquals(2, days.size)
        assertTrue(days.contains(day1))
        assertTrue(days.contains(day2))
    }
}
