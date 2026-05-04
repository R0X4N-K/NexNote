package com.example.nexnote.ui.screen.agenda

import com.example.nexnote.data.db.entity.NoteEntity
import com.example.nexnote.data.repository.NoteRepository
import com.example.nexnote.domain.usecase.MoveNoteToTrashUseCase
import com.example.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import com.example.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import com.example.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import com.example.nexnote.domain.usecase.ToggleNotePinUseCase
import com.example.nexnote.ui.common.NoteListViewMode
import com.example.nexnote.ui.common.SortOrder
import com.example.nexnote.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: AgendaFakeNoteDao
    private lateinit var viewModel: AgendaViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = AgendaFakeNoteDao()
        val repository = NoteRepository(fakeDao)
        viewModel = AgendaViewModel(
            observeDistinctLocalDays = ObserveDistinctLocalDaysUseCase(repository),
            observeNotesByDateRange = ObserveNotesByDateRangeUseCase(repository),
            moveNoteToTrash = MoveNoteToTrashUseCase(repository),
            restoreNoteFromTrash = RestoreNoteFromTrashUseCase(repository),
            toggleNotePin = ToggleNotePinUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        block()
    }

    @Test
    fun `initialValue has isLoading true before subscription`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `isLoading false after first emission`() = runViewModelTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `initial state reflects today`() = runViewModelTest {
        advanceUntilIdle()
        val today = Calendar.getInstance()
        val state = viewModel.uiState.value
        assertEquals(today.get(Calendar.YEAR), state.displayedYear)
        assertEquals(today.get(Calendar.MONTH), state.displayedMonth)
        assertEquals(today.get(Calendar.YEAR), state.selectedYear)
        assertEquals(today.get(Calendar.MONTH), state.selectedMonth)
        assertEquals(today.get(Calendar.DAY_OF_MONTH), state.selectedDay)
    }

    @Test
    fun `navigateToPreviousMonth decrements month`() = runViewModelTest {
        viewModel.selectDate(2025, Calendar.MARCH, 15)
        advanceUntilIdle()
        viewModel.navigateToPreviousMonth()
        advanceUntilIdle()
        assertEquals(Calendar.FEBRUARY, viewModel.uiState.value.displayedMonth)
        assertEquals(2025, viewModel.uiState.value.displayedYear)
    }

    @Test
    fun `navigateToPreviousMonth wraps year from January to December`() = runViewModelTest {
        viewModel.selectDate(2025, Calendar.JANUARY, 10)
        advanceUntilIdle()
        viewModel.navigateToPreviousMonth()
        advanceUntilIdle()
        assertEquals(Calendar.DECEMBER, viewModel.uiState.value.displayedMonth)
        assertEquals(2024, viewModel.uiState.value.displayedYear)
    }

    @Test
    fun `navigateToNextMonth increments month`() = runViewModelTest {
        viewModel.selectDate(2025, Calendar.MARCH, 15)
        advanceUntilIdle()
        viewModel.navigateToNextMonth()
        advanceUntilIdle()
        assertEquals(Calendar.APRIL, viewModel.uiState.value.displayedMonth)
        assertEquals(2025, viewModel.uiState.value.displayedYear)
    }

    @Test
    fun `navigateToNextMonth wraps year from December to January`() = runViewModelTest {
        viewModel.selectDate(2024, Calendar.DECEMBER, 10)
        advanceUntilIdle()
        viewModel.navigateToNextMonth()
        advanceUntilIdle()
        assertEquals(Calendar.JANUARY, viewModel.uiState.value.displayedMonth)
        assertEquals(2025, viewModel.uiState.value.displayedYear)
    }

    @Test
    fun `clamp selectedDay when navigating to shorter month`() = runViewModelTest {
        viewModel.selectDate(2025, Calendar.JANUARY, 31)
        advanceUntilIdle()
        viewModel.navigateToNextMonth()
        advanceUntilIdle()
        assertEquals(28, viewModel.uiState.value.selectedDay)
        assertEquals(Calendar.FEBRUARY, viewModel.uiState.value.selectedMonth)
        assertEquals(2025, viewModel.uiState.value.selectedYear)
    }

    @Test
    fun `no clamp when navigating to month with enough days`() = runViewModelTest {
        viewModel.selectDate(2025, Calendar.JANUARY, 28)
        advanceUntilIdle()
        viewModel.navigateToNextMonth()
        advanceUntilIdle()
        assertEquals(28, viewModel.uiState.value.selectedDay)
    }

    @Test
    fun `selectDate updates all date fields and displayed month`() = runViewModelTest {
        viewModel.selectDate(2024, Calendar.JUNE, 15)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2024, state.selectedYear)
        assertEquals(Calendar.JUNE, state.selectedMonth)
        assertEquals(15, state.selectedDay)
        assertEquals(2024, state.displayedYear)
        assertEquals(Calendar.JUNE, state.displayedMonth)
    }

    @Test
    fun `daysWithNotes populated from creation dates`() = runViewModelTest {
        val noteTs = DateUtils.toMillis(2025, Calendar.MARCH, 10)
        fakeDao.addNote(NoteEntity(id = 1L, creationDate = noteTs))
        advanceUntilIdle()
        val expected = DateUtils.startOfDay(noteTs)
        assertTrue(expected in viewModel.uiState.value.daysWithNotes)
    }

    @Test
    fun `daysWithNotes updates when new note is added`() = runViewModelTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.daysWithNotes.isEmpty())

        val noteTs = DateUtils.toMillis(2025, Calendar.APRIL, 5)
        fakeDao.addNote(NoteEntity(id = 2L, creationDate = noteTs))
        advanceUntilIdle()

        val expected = DateUtils.startOfDay(noteTs)
        assertTrue(expected in viewModel.uiState.value.daysWithNotes)
    }

    @Test
    fun `notesForSelectedDate returns notes created on selected day`() = runViewModelTest {
        val noteTs = DateUtils.toMillis(2025, Calendar.MARCH, 15)
        fakeDao.addNote(NoteEntity(id = 10L, title = "Note on the 15th", creationDate = noteTs))
        viewModel.selectDate(2025, Calendar.MARCH, 15)
        advanceUntilIdle()
        val notes = viewModel.uiState.value.notesForSelectedDate
        assertEquals(1, notes.size)
        assertEquals(10L, notes.first().id)
    }

    @Test
    fun `notesForSelectedDate empty when no notes for selected day`() = runViewModelTest {
        val noteTs = DateUtils.toMillis(2025, Calendar.MARCH, 15)
        fakeDao.addNote(NoteEntity(id = 11L, creationDate = noteTs))
        viewModel.selectDate(2025, Calendar.MARCH, 16)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notesForSelectedDate.isEmpty())
    }

    @Test
    fun `initial isSearchActive is false`() = runViewModelTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `onSearchToggle true activates search`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.onSearchToggle(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `onSearchToggle false clears query`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.onSearchToggle(true)
        viewModel.onSearchQueryChange("test")
        advanceUntilIdle()
        viewModel.onSearchToggle(false)
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertFalse(viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `search filters notes for selected day`() = runViewModelTest {
        val noteTs = DateUtils.toMillis(2025, Calendar.JUNE, 10)
        fakeDao.addNote(NoteEntity(id = 20L, title = "Kotlin note", creationDate = noteTs))
        fakeDao.addNote(NoteEntity(id = 21L, title = "Java note", creationDate = noteTs))
        viewModel.selectDate(2025, Calendar.JUNE, 10)
        advanceUntilIdle()

        viewModel.onSearchToggle(true)
        viewModel.onSearchQueryChange("Kotlin")
        advanceTimeBy(350)
        advanceUntilIdle()

        val notes = viewModel.uiState.value.notesForSelectedDate
        assertEquals(1, notes.size)
        assertEquals(20L, notes.first().id)
    }

    @Test
    fun `initial sortOrder is MODIFIED_DESC`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(SortOrder.MODIFIED_DESC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `toggleSortOrder switches to MODIFIED_ASC`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.toggleSortOrder()
        advanceUntilIdle()
        assertEquals(SortOrder.MODIFIED_ASC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `toggleSortOrder switches back to MODIFIED_DESC`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.toggleSortOrder()
        viewModel.toggleSortOrder()
        advanceUntilIdle()
        assertEquals(SortOrder.MODIFIED_DESC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `initial viewMode is LIST`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(NoteListViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggleViewMode switches to GRID`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals(NoteListViewMode.GRID, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggleViewMode switches back to LIST`() = runViewModelTest {
        advanceUntilIdle()
        viewModel.toggleViewMode()
        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals(NoteListViewMode.LIST, viewModel.uiState.value.viewMode)
    }
}
