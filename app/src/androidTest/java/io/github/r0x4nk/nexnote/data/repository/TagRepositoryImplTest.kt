package io.github.r0x4nk.nexnote.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagRepositoryImplTest {

    private lateinit var db: NexNoteDatabase
    private lateinit var repository: TagRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TagRepositoryImpl(
            database = db,
            tagDao = db.tagDao(),
            noteContentPatchDao = db.noteContentPatchDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun indexNoteTags_indexesNormalNote() = runTest {
        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal",
                content = "Visible #work",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        repository.indexNoteTags(noteId, "Visible #work")

        assertEquals(
            listOf("work"),
            db.tagDao().getCrossRefsForNote(noteId).map { it.tagName }
        )
        assertEquals(listOf("work"), repository.searchTags("work").first().map { it.name })
    }

    @Test
    fun indexNoteTags_scrubsVaultNoteCrossRefsWithoutIndexingContent() = runTest {
        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Encrypted title placeholder",
                content = "Encrypted content placeholder",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L,
                isInVault = true
            )
        )
        db.tagDao().insertTag(
            TagEntity(name = "oldsecret", createdDate = 1_000L, lastUpdatedDate = 1_000L)
        )
        db.tagDao().insertCrossRef(NoteTagCrossRef(noteId = noteId, tagName = "oldsecret"))

        repository.indexNoteTags(noteId, "Private #newsecret")

        assertTrue(db.tagDao().getCrossRefsForNote(noteId).isEmpty())
        assertTrue(repository.searchTags("secret").first().isEmpty())
    }

    @Test
    fun observeNotesForTag_selectsOnlyMatchingActiveNormalNotes() = runTest {
        val normalId = db.noteDao().insertNote(
            NoteEntity(title = "Normal", content = "#work", lastModifiedDate = 3_000L)
        )
        val unrelatedId = db.noteDao().insertNote(
            NoteEntity(title = "Unrelated", content = "#home", lastModifiedDate = 2_000L)
        )
        val deletedId = db.noteDao().insertNote(
            NoteEntity(title = "Deleted", content = "#work", isDeleted = true)
        )
        val vaultId = db.noteDao().insertNote(
            NoteEntity(title = "Vault", content = "#work", isInVault = true)
        )
        db.tagDao().insertTag(TagEntity("work", 1_000L, 1_000L))
        listOf(normalId, deletedId, vaultId).forEach { noteId ->
            db.tagDao().insertCrossRef(NoteTagCrossRef(noteId, "work"))
        }
        db.tagDao().insertTag(TagEntity("home", 1_000L, 1_000L))
        db.tagDao().insertCrossRef(NoteTagCrossRef(unrelatedId, "home"))

        assertEquals(
            listOf(normalId),
            repository.observeNotesForTag("work").first().map { note -> note.id }
        )
    }

    @Test
    fun deleteTag_scrubsStaleVaultCrossRefsWithoutPatchingVaultContent() = runTest {
        val normalNoteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal",
                content = "Visible #shared",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )
        val vaultNoteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Encrypted title placeholder",
                content = "Encrypted #shared placeholder",
                creationDate = 2_000L,
                lastModifiedDate = 2_000L,
                isInVault = true
            )
        )
        db.tagDao().insertTag(
            TagEntity(name = "shared", createdDate = 1_000L, lastUpdatedDate = 1_000L)
        )
        db.tagDao().insertCrossRef(NoteTagCrossRef(noteId = normalNoteId, tagName = "shared"))
        db.tagDao().insertCrossRef(NoteTagCrossRef(noteId = vaultNoteId, tagName = "shared"))

        repository.deleteTag("shared")

        assertEquals("Visible shared", db.noteDao().getNoteById(normalNoteId)?.content)
        assertEquals(
            "Encrypted #shared placeholder",
            db.noteDao().getVaultNoteById(vaultNoteId)?.content
        )
        assertTrue(db.tagDao().getCrossRefsForNote(normalNoteId).isEmpty())
        assertTrue(db.tagDao().getCrossRefsForNote(vaultNoteId).isEmpty())
        assertTrue(repository.searchTags("shared").first().isEmpty())
    }
}
