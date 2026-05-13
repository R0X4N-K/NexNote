package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import io.github.r0x4nk.nexnote.ui.navigation.Screen
import io.github.r0x4nk.nexnote.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeNoteDao
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao    = FakeNoteDao()
        repository = NoteRepositoryImpl(fakeDao, NoOpNoteImageStorage())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(noteId: Long = Screen.NO_ID) =
        ExportViewModel(
            getNoteById = GetNoteByIdUseCase(repository),
            observeAllNotes = ObserveAllNotesUseCase(repository),
            observeNotesByDateRange = ObserveNotesByDateRangeUseCase(repository),
            initialNoteId = noteId
        )

    // ── Initial scope ────────────────────────────────────────────────────────

    @Test
    fun `initial scope is SingleNote when noteId provided`() = runTest {
        val vm = viewModel(noteId = 42L)
        assertEquals(ExportScope.SingleNote, vm.uiState.value.scope)
    }

    @Test
    fun `initial scope is AllNotes when no noteId`() = runTest {
        val vm = viewModel()
        assertEquals(ExportScope.AllNotes, vm.uiState.value.scope)
    }

    // ── isLoading ─────────────────────────────────────────────────────────────

    @Test
    fun `isLoading false after initial load`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── selectScope ───────────────────────────────────────────────────────────

    @Test
    fun `selectScope updates scope and reloads notes`() = runTest {
        fakeDao.addNote(NoteEntity(id = 1L, title = "A"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectScope(ExportScope.AllNotes)
        advanceUntilIdle()

        assertEquals(ExportScope.AllNotes, vm.uiState.value.scope)
        assertEquals(1, vm.uiState.value.notes.size)
    }

    // ── selectFormat ──────────────────────────────────────────────────────────

    @Test
    fun `selectFormat changes format without reloading notes`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectFormat(ExportFormat.PDF)

        assertEquals(ExportFormat.PDF, vm.uiState.value.format)
        // isLoading remains false: no reload.
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── AllNotes scope ────────────────────────────────────────────────────────

    @Test
    fun `AllNotes scope returns all active notes`() = runTest {
        fakeDao.addNote(NoteEntity(id = 1L, title = "Note 1"))
        fakeDao.addNote(NoteEntity(id = 2L, title = "Note 2"))
        fakeDao.addNote(NoteEntity(id = 3L, title = "Deleted", isDeleted = true))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.notes.size)
    }

    // ── SingleNote scope ──────────────────────────────────────────────────────

    @Test
    fun `SingleNote scope loads correct note by id`() = runTest {
        fakeDao.addNote(NoteEntity(id = 10L, title = "Target note"))
        fakeDao.addNote(NoteEntity(id = 11L, title = "Other note"))

        val vm = viewModel(noteId = 10L)
        advanceUntilIdle()

        val notes = vm.uiState.value.notes
        assertEquals(1, notes.size)
        assertEquals(10L, notes.first().id)
    }

    // ── DateRange scope ───────────────────────────────────────────────────────

    @Test
    fun `DateRange includes notes on dateTo day (inclusive end)`() = runTest {
        // Note created at noon on June 15, 2025
        val noonJune15 = DateUtils.toMillis(2025, Calendar.JUNE, 15)
        fakeDao.addNote(NoteEntity(id = 1L, creationDate = noonJune15))

        val vm = viewModel()
        advanceUntilIdle()

        // from = start of June 1, to = start of June 15
        // → startOfNextDay(June 15) = midnight June 16 → note is included
        val from = DateUtils.startOfDay(DateUtils.toMillis(2025, Calendar.JUNE, 1))
        val to   = DateUtils.startOfDay(noonJune15)
        vm.selectScope(ExportScope.DateRange)
        vm.selectDateRange(from, to)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.notes.size)
    }

    @Test
    fun `DateRange excludes notes after dateTo day`() = runTest {
        val june16 = DateUtils.toMillis(2025, Calendar.JUNE, 16)
        fakeDao.addNote(NoteEntity(id = 1L, creationDate = june16))

        val vm = viewModel()
        advanceUntilIdle()

        // to = June 15 → note from June 16 is excluded
        val from = DateUtils.startOfDay(DateUtils.toMillis(2025, Calendar.JUNE, 1))
        val to   = DateUtils.startOfDay(DateUtils.toMillis(2025, Calendar.JUNE, 15))
        vm.selectScope(ExportScope.DateRange)
        vm.selectDateRange(from, to)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notes.isEmpty())
    }

    @Test
    fun `DateRange with no dates set returns empty list`() = runTest {
        fakeDao.addNote(NoteEntity(id = 1L))
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectScope(ExportScope.DateRange)
        // No selectDateRange call: dateFrom and dateTo are null.
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notes.isEmpty())
    }

    // ── Export state ─────────────────────────────────────────────────────────

    @Test
    fun `onExportError sets error message and clears isExporting`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onExportStart()
        assertTrue(vm.uiState.value.isExporting)

        vm.onExportError("Test error")
        assertFalse(vm.uiState.value.isExporting)
        assertEquals("Test error", vm.uiState.value.error)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        val vm = viewModel()
        vm.onExportError("error")
        vm.clearError()
        assertEquals(null, vm.uiState.value.error)
    }
}

// ── Fake ─────────────────────────────────────────────────────────────────────

private class FakeNoteDao : NoteDao {

    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())

    fun addNote(note: NoteEntity) {
        _notes.value = _notes.value + note
    }

    override fun getAllNotes(): Flow<List<NoteEntity>> =
        _notes.map { list -> list.filter { !it.isDeleted } }

    override fun getAllNotesSortedAsc(): Flow<List<NoteEntity>> =
        _notes.map { list -> list.filter { !it.isDeleted } }

    override fun getDeletedNotes(): Flow<List<NoteEntity>> =
        _notes.map { list -> list.filter { it.isDeleted } }

    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        _notes.map { list ->
            list
                .filter { !it.isDeleted }
                .map { NoteLinkCandidateProjection(id = it.id, title = it.title) }
        }

    override fun getAllCreationDates(): Flow<List<Long>> =
        _notes.map { list -> list.filter { !it.isDeleted }.map { it.creationDate } }

    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<NoteEntity>> =
        _notes.map { list ->
            list.filter { !it.isDeleted && it.creationDate in startMs until endMs }
        }

    override fun searchNotes(query: String): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun getNoteById(id: Long): NoteEntity? =
        _notes.value.find { it.id == id && !it.isDeleted }

    override suspend fun insertNote(note: NoteEntity): Long         = 0L
    override suspend fun updateNote(note: NoteEntity)               = Unit
    override suspend fun moveToTrash(id: Long, deletedDate: Long)   = Unit
    override suspend fun restoreFromTrash(id: Long)                 = Unit
    override suspend fun deleteNotePermanently(id: Long): Int       = 0
    override suspend fun emptyTrash(): Int                          = 0
    override suspend fun getDeletedImagePathsRaw(): List<String>    = emptyList()
    override suspend fun setPinned(id: Long, isPinned: Boolean)     = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
}
