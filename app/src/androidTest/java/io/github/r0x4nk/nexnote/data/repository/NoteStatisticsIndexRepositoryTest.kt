package io.github.r0x4nk.nexnote.data.repository

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.domain.model.HomeNotesQuery
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NoteStatisticsIndexRepositoryTest {

    private lateinit var database: NexNoteDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .addCallback(NexNoteDatabase.NOTE_SEARCH_SYNC_CALLBACK)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveNoteUpdatesStatisticsIndexInSamePersistenceBoundary() = runTest {
        val repository = NoteRepositoryImpl(
            dao = database.noteDao(),
            imageStorage = NoOpNoteImageStorage(),
            database = database,
            statisticsDao = database.noteStatisticsDao(),
            homeNoteDao = database.homeNoteDao()
        )

        val id = repository.saveNote(
            Note(content = "Alpha beta #work", creationDate = 123L)
        )
        val first = database.noteStatisticsDao()
            .observeIndexedNotes(NoteStatisticsRepositoryImpl.INDEX_FORMAT_VERSION)
            .first()
            .single()
        assertEquals(id, first.noteId)
        assertEquals(3, first.wordCount)

        repository.saveNote(
            repository.getNoteById(id)!!.copy(content = "One two three four")
        )
        val updated = database.noteStatisticsDao()
            .observeIndexedNotes(NoteStatisticsRepositoryImpl.INDEX_FORMAT_VERSION)
            .first()
            .single()
        assertEquals(4, updated.wordCount)
        assertEquals("", updated.tagNamesRaw)
    }

    @Test
    fun backgroundIndexerBuildsMissingRowsAndCanRebuildThem() = runTest {
        database.noteDao().insertNote(
            NoteEntity(content = "Existing note #legacy", lastModifiedDate = 200L)
        )
        val repository = NoteStatisticsRepositoryImpl(
            dao = database.noteStatisticsDao(),
            appScope = backgroundScope,
            computationDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        repository.start()

        val completed = repository.indexState.first { state ->
            state.totalNotes == 1 && !state.isIndexing
        }
        assertEquals(1, completed.indexedNotes)

        repository.rebuildIndex()
        val rebuilt = repository.indexState.first { state ->
            state.totalNotes == 1 && !state.isIndexing
        }
        assertFalse(rebuilt.isRetryingAfterError)
        assertEquals(1, repository.indexedNotes.first().size)
    }

    @Test
    fun homeSearchUsesFtsScopePinnedStateTagIntersectionAndSort() = runTest {
        val repository = NoteRepositoryImpl(
            dao = database.noteDao(),
            imageStorage = NoOpNoteImageStorage(),
            database = database,
            statisticsDao = database.noteStatisticsDao(),
            homeNoteDao = database.homeNoteDao()
        )
        val roadmapId = repository.saveNote(
            Note(title = "Kotlin roadmap", content = "Shared plan #work")
        )
        val archiveId = repository.saveNote(
            Note(title = "Kotlin archive", content = "Reference", isPinned = true)
        )
        val contentId = repository.saveNote(
            Note(title = "Platform notes", content = "Kotlin internals")
        )
        database.tagDao().insertTag(TagEntity("work"))
        database.tagDao().insertCrossRef(NoteTagCrossRef(roadmapId, "work"))

        val titles = repository.observeHomeNotes(
            HomeNotesQuery(
                text = "kot",
                searchScope = HomeSearchScope.TITLE,
                searchSort = HomeSearchSort.TITLE_ASC,
                limit = 64
            )
        ).first()
        assertEquals(listOf(archiveId, roadmapId), titles.map(Note::id))

        val pinned = repository.observeHomeNotes(
            HomeNotesQuery(
                text = "kotlin",
                pinnedFilter = HomePinnedFilter.PINNED,
                limit = 64
            )
        ).first()
        assertEquals(listOf(archiveId), pinned.map(Note::id))

        val content = repository.observeHomeNotes(
            HomeNotesQuery(
                text = "kotlin",
                searchScope = HomeSearchScope.CONTENT,
                limit = 64
            )
        ).first()
        assertEquals(listOf(contentId), content.map(Note::id))

        val tagged = repository.observeHomeNotes(
            HomeNotesQuery(text = "kotlin", tagNames = setOf("work"), limit = 64)
        ).first()
        assertEquals(listOf(roadmapId), tagged.map(Note::id))
    }

    @Test
    fun searchIndexTracksOnlyActiveNormalNotes() = runTest {
        val normalId = database.noteDao().insertNote(
            NoteEntity(title = "Visible searchable note")
        )
        database.noteDao().insertNote(
            NoteEntity(title = "Deleted searchable note", isDeleted = true)
        )
        database.noteDao().insertNote(
            NoteEntity(title = "Vault searchable note", isInVault = true)
        )

        assertEquals(1, ftsMatchCount("visible*"))
        assertEquals(0, ftsMatchCount("deleted*"))
        assertEquals(0, ftsMatchCount("vault*"))

        database.noteDao().moveToTrash(normalId, deletedDate = 100L)
        assertEquals(0, ftsMatchCount("visible*"))
        database.noteDao().restoreFromTrash(normalId)
        assertEquals(1, ftsMatchCount("visible*"))
    }

    @Test
    fun largeDatasetKeepsHomeBoundedAndIndexesAllRows() = runTest {
        val noteCount = 26_500
        database.withTransaction {
            repeat(noteCount) { index ->
                database.noteDao().insertNote(
                    NoteEntity(
                        title = "Note $index",
                        content = "Body $index #bulk",
                        creationDate = index.toLong(),
                        lastModifiedDate = index.toLong()
                    )
                )
            }
        }
        val noteRepository = NoteRepositoryImpl(
            dao = database.noteDao(),
            imageStorage = NoOpNoteImageStorage(),
            database = database,
            statisticsDao = database.noteStatisticsDao(),
            homeNoteDao = database.homeNoteDao()
        )

        val window = noteRepository.observeHomeNotes(HomeNotesQuery(limit = 64)).first()
        assertEquals(64, window.size)
        assertEquals(noteCount, noteRepository.activeNoteCount.first())
        val selectionIds = noteRepository.observeHomeNoteIds(
            HomeNotesQuery(limit = 1)
        ).first()
        assertEquals(noteCount, selectionIds.size)
        assertEquals(noteCount, selectionIds.toSet().size)
        val searchWindow = noteRepository.observeHomeNotes(
            HomeNotesQuery(
                text = "26499",
                searchScope = HomeSearchScope.TITLE,
                limit = 64
            )
        ).first()
        assertEquals(listOf("Note 26499"), searchWindow.map { note -> note.title })

        val statisticsRepository = NoteStatisticsRepositoryImpl(
            dao = database.noteStatisticsDao(),
            appScope = backgroundScope,
            computationDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        statisticsRepository.start()
        val completed = statisticsRepository.indexState.first { state ->
            state.totalNotes == noteCount && !state.isIndexing
        }

        assertEquals(noteCount, completed.indexedNotes)
        assertEquals(noteCount, statisticsRepository.indexedNotes.first().size)

        noteRepository.saveNote(Note(title = "After large import", content = "Still responsive"))
        val afterInsert = statisticsRepository.indexState.first { state ->
            state.totalNotes == noteCount + 1 && !state.isIndexing
        }
        assertEquals(noteCount + 1, afterInsert.indexedNotes)
        assertEquals(64, noteRepository.observeHomeNotes(HomeNotesQuery(limit = 64)).first().size)

        val everyActiveId = noteRepository.observeHomeNoteIds(HomeNotesQuery(limit = 1)).first()
        noteRepository.moveToTrash(everyActiveId)
        assertEquals(0, noteRepository.activeNoteCount.first())
        assertEquals(noteCount + 1, noteRepository.deletedNotes.first().size)

        noteRepository.restoreFromTrash(everyActiveId)
        assertEquals(noteCount + 1, noteRepository.activeNoteCount.first())
    }

    @Test
    fun deleteAllNormalNotesRemovesActiveAndTrashButPreservesVaultRows() = runTest {
        val activeId = database.noteDao().insertNote(NoteEntity(title = "Active"))
        val trashedId = database.noteDao().insertNote(NoteEntity(title = "Trashed"))
        database.noteDao().moveToTrash(trashedId, deletedDate = 100L)
        val vaultId = database.noteDao().insertNote(
            NoteEntity(title = "Encrypted placeholder", isInVault = true)
        )
        val repository = NoteRepositoryImpl(
            dao = database.noteDao(),
            imageStorage = NoOpNoteImageStorage(),
            database = database,
            statisticsDao = database.noteStatisticsDao(),
            homeNoteDao = database.homeNoteDao()
        )

        assertEquals(2, repository.allNormalNoteCount.first())
        assertEquals(1, database.noteDao().observeAllVaultNoteCount().first())

        val removed = repository.deleteAllNormalNotesPermanently()

        assertEquals(2, removed)
        assertEquals(0, repository.allNormalNoteCount.first())
        assertEquals(1, database.noteDao().observeAllVaultNoteCount().first())
        assertEquals(vaultId, database.noteDao().getVaultNoteById(vaultId)?.id)
        assertEquals(null, database.noteDao().getNoteById(activeId))
    }

    private fun ftsMatchCount(matchQuery: String): Int =
        database.openHelper.writableDatabase.query(
            "SELECT COUNT(*) FROM notes_fts WHERE notes_fts MATCH ?",
            arrayOf(matchQuery)
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
