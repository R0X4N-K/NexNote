package io.github.r0x4nk.nexnote.ui.screen.home

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.data.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesSortedAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchNotesScoredUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeNoteDao
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeNoteDao()
        val repository = NoteRepository(fakeDao, NoOpNoteImageStorage())
        viewModel = HomeViewModel(
            searchNotesScored = SearchNotesScoredUseCase(repository),
            observeAllNotesSortedAsc = ObserveAllNotesSortedAscUseCase(repository),
            observeAllNotes = ObserveAllNotesUseCase(repository),
            moveNoteToTrash = MoveNoteToTrashUseCase(repository),
            restoreNoteFromTrash = RestoreNoteFromTrashUseCase(repository),
            toggleNotePin = ToggleNotePinUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Subscribes to uiState to activate WhileSubscribed sharing, then runs [block]. */
    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        block()
    }

    // ── M8: Search with scoring ──────────────────────────────────────────────

    @Test
    fun `search emits scoredResults when query is not blank`() = runViewModelTest {
        // Popola le note nel dao
        fakeDao.emitAllNotes(listOf(
            NoteEntity(id = 1L, title = "Kotlin tips", content = "Hello"),
            NoteEntity(id = 2L, title = "Java basics", content = "Kotlin intro")
        ))
        fakeDao.emitSearchNotes(listOf(
            NoteEntity(id = 1L, title = "Kotlin tips", content = "Hello"),
            NoteEntity(id = 2L, title = "Java basics", content = "Kotlin intro")
        ))
        advanceUntilIdle()

        // Activate search and type query
        viewModel.onSearchToggle(true)
        viewModel.onSearchQueryChange("Kotlin")
        advanceTimeBy(350) // debounce 300ms
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("scoredResults should not be empty", state.scoredResults.isNotEmpty())
        // The note with "Kotlin" in the title must have a higher score
        assertEquals(1L, state.scoredResults.first().note.id)
        assertTrue(state.scoredResults[0].score > state.scoredResults[1].score)
    }

    @Test
    fun `searchToggle off clears scoredResults`() = runViewModelTest {
        fakeDao.emitAllNotes(listOf(
            NoteEntity(id = 1L, title = "Test", content = "content")
        ))
        fakeDao.emitSearchNotes(listOf(
            NoteEntity(id = 1L, title = "Test", content = "content")
        ))
        advanceUntilIdle()

        // Activate then deactivate search
        viewModel.onSearchToggle(true)
        viewModel.onSearchQueryChange("test")
        advanceTimeBy(350)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.scoredResults.isNotEmpty())

        viewModel.onSearchToggle(false) // toggle off → query reset
        advanceTimeBy(350)
        advanceUntilIdle()
        assertTrue(
            "scoredResults should be empty after toggle off",
            viewModel.uiState.value.scoredResults.isEmpty()
        )
    }

    // ── View mode ────────────────────────────────────────────────────────────

    @Test
    fun `initial viewMode is LIST`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(NoteListViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggleViewMode switches from LIST to GRID`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals(NoteListViewMode.GRID, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggleViewMode switches back from GRID to LIST`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.toggleViewMode()
        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals(NoteListViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    // ── Undo trash ──────────────────────────────────────────────────────────

    @Test
    fun `undoPendingTrash calls restoreFromTrash on the dao`() = runViewModelTest {
        viewModel.undoPendingTrash(42L)
        advanceUntilIdle()
        assertEquals(42L, fakeDao.lastRestoredId)
    }

    @Test
    fun `requestTrash emits event with readable note label`() = runViewModelTest {
        val event = async { viewModel.trashEvents.first() }
        val note = Note(id = 7L, title = "", content = "\n  Draft   outline  ")

        viewModel.requestTrash(note)
        advanceUntilIdle()

        val trashEvent = event.await()
        assertEquals(7L, fakeDao.lastTrashedId)
        assertEquals(7L, trashEvent.noteId)
        assertEquals("Draft outline", trashEvent.noteLabel)
    }

    @Test
    fun `requestTrash followed by undo restores note to active list`() = runViewModelTest {
        fakeDao.emitAllNotes(
            listOf(NoteEntity(id = 9L, title = "Recoverable note", lastModifiedDate = 100L))
        )
        advanceUntilIdle()

        val note = viewModel.uiState.value.notes.single()
        viewModel.requestTrash(note)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notes.isEmpty())

        viewModel.undoPendingTrash(note.id)
        advanceUntilIdle()

        assertEquals(
            listOf(9L),
            viewModel.uiState.value.notes.map { it.id }
        )
    }

    @Test
    fun `debounce does not emit immediately`() = runViewModelTest {
        fakeDao.emitAllNotes(listOf(
            NoteEntity(id = 1L, title = "Test", content = "content")
        ))
        fakeDao.emitSearchNotes(listOf(
            NoteEntity(id = 1L, title = "Test", content = "content")
        ))
        advanceUntilIdle()

        viewModel.onSearchToggle(true)
        viewModel.onSearchQueryChange("test")

        // Advance only 100ms — debounce is 300ms, so the query hasn't been processed yet.
        // Do NOT call advanceUntilIdle here, as it would process all pending work.
        advanceTimeBy(100)

        assertTrue(
            "scoredResults should be empty before debounce completes",
            viewModel.uiState.value.scoredResults.isEmpty()
        )

        // Now advance past debounce threshold
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(
            "scoredResults should be populated after debounce",
            viewModel.uiState.value.scoredResults.isNotEmpty()
        )
    }
}

// ── Fake ─────────────────────────────────────────────────────────────────────

private class FakeNoteDao : NoteDao {

    private val _allNotes    = MutableStateFlow<List<NoteEntity>>(emptyList())
    private val _deletedNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    private val _searchNotes = MutableStateFlow<List<NoteEntity>>(emptyList())

    var lastRestoredId: Long? = null
    var lastTrashedId: Long? = null

    fun emitAllNotes(notes: List<NoteEntity>) { _allNotes.value = notes }
    fun emitSearchNotes(notes: List<NoteEntity>) { _searchNotes.value = notes }

    override fun getAllNotes(): Flow<List<NoteEntity>> = _allNotes
    override fun getAllNotesSortedAsc(): Flow<List<NoteEntity>> = _allNotes
    override fun getDeletedNotes(): Flow<List<NoteEntity>> = _deletedNotes
    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        _allNotes.map { list ->
            list
                .filter { !it.isDeleted }
                .map { NoteLinkCandidateProjection(id = it.id, title = it.title) }
        }
    override fun getAllCreationDates(): Flow<List<Long>> = MutableStateFlow(emptyList())

    override suspend fun getNoteById(id: Long): NoteEntity? = null
    override fun searchNotes(query: String): Flow<List<NoteEntity>> = _searchNotes
    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun insertNote(note: NoteEntity): Long = 0L
    override suspend fun updateNote(note: NoteEntity) = Unit
    override suspend fun moveToTrash(id: Long, deletedDate: Long) {
        lastTrashedId = id
        val trashedNote = _allNotes.value.find { it.id == id }
            ?.copy(isDeleted = true, deletedDate = deletedDate)
            ?: return
        _allNotes.value = _allNotes.value.filterNot { it.id == id }
        _deletedNotes.value = _deletedNotes.value + trashedNote
    }
    override suspend fun restoreFromTrash(id: Long) {
        lastRestoredId = id
        val restoredNote = _deletedNotes.value.find { it.id == id }
            ?.copy(isDeleted = false, deletedDate = null)
            ?: return
        _deletedNotes.value = _deletedNotes.value.filterNot { it.id == id }
        _allNotes.value = _allNotes.value + restoredNote
    }
    override suspend fun deleteNotePermanently(id: Long): Int = 0
    override suspend fun emptyTrash(): Int = 0
    override suspend fun getDeletedImagePathsRaw(): List<String> = emptyList()
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
}
