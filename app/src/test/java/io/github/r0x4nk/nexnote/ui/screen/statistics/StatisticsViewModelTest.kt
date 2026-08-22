package io.github.r0x4nk.nexnote.ui.screen.statistics

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteStatisticsRepository
import io.github.r0x4nk.nexnote.domain.usecase.BuildNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveIndexedNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteStatisticsIndexStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.testing.NoOpPreferencesRepository
import io.github.r0x4nk.nexnote.testing.NoOpTagRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `year navigation selects prepared snapshots`() = runTest {
        val notes = listOf(
            note(1, LocalDate.of(2025, 5, 12)),
            note(2, LocalDate.of(2026, 8, 21))
        )
        val repository = StatisticsFakeRepository(notes.map(Note::toIndexedStatistics))
        val viewModel = StatisticsViewModel(
            observeIndexedNotes = ObserveIndexedNoteStatisticsUseCase(repository),
            observeIndexState = ObserveNoteStatisticsIndexStateUseCase(repository),
            observeTags = ObserveTagsByUsageDescUseCase(NoOpTagRepository),
            observeTimezoneId = ObserveTimezoneIdUseCase(NoOpPreferencesRepository),
            buildStatistics = BuildNoteStatisticsUseCase(),
            nowMillis = { instant(LocalDate.of(2026, 8, 21)) },
            computationDispatcher = testDispatcher
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(2026, viewModel.uiState.value.statistics?.selectedYear)
        assertEquals(1, viewModel.uiState.value.statistics?.totalNotes)

        viewModel.showPreviousYear()
        advanceUntilIdle()

        assertEquals(2025, viewModel.uiState.value.statistics?.selectedYear)
        assertEquals(1, viewModel.uiState.value.statistics?.totalNotes)

        viewModel.showNextYear()
        advanceUntilIdle()

        assertEquals(2026, viewModel.uiState.value.statistics?.selectedYear)
    }

    @Test
    fun `first note is rendered after Room rows and counters become coherent`() = runTest {
        val repository = StatisticsFakeRepository(emptyList())
        val viewModel = StatisticsViewModel(
            observeIndexedNotes = ObserveIndexedNoteStatisticsUseCase(repository),
            observeIndexState = ObserveNoteStatisticsIndexStateUseCase(repository),
            observeTags = ObserveTagsByUsageDescUseCase(NoOpTagRepository),
            observeTimezoneId = ObserveTimezoneIdUseCase(NoOpPreferencesRepository),
            buildStatistics = BuildNoteStatisticsUseCase(),
            nowMillis = { instant(LocalDate.of(2026, 8, 21)) },
            computationDispatcher = testDispatcher
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        repository.setIndexState(indexedNotes = 1, totalNotes = 1)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.statistics?.totalNotes)

        repository.setNotes(listOf(note(1, LocalDate.of(2026, 8, 21)).toIndexedStatistics()))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.statistics?.totalNotes)
        assertEquals(1, viewModel.uiState.value.processedNotes)
        assertEquals(1, viewModel.uiState.value.totalNotes)
    }

    @Test
    fun `single note outside current year selects the year that contains it`() = runTest {
        val indexedNote = note(1, LocalDate.of(2025, 5, 12)).toIndexedStatistics()
        val repository = StatisticsFakeRepository(listOf(indexedNote))
        val viewModel = StatisticsViewModel(
            observeIndexedNotes = ObserveIndexedNoteStatisticsUseCase(repository),
            observeIndexState = ObserveNoteStatisticsIndexStateUseCase(repository),
            observeTags = ObserveTagsByUsageDescUseCase(NoOpTagRepository),
            observeTimezoneId = ObserveTimezoneIdUseCase(NoOpPreferencesRepository),
            buildStatistics = BuildNoteStatisticsUseCase(),
            nowMillis = { instant(LocalDate.of(2026, 8, 21)) },
            computationDispatcher = testDispatcher
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(2025, viewModel.uiState.value.statistics?.selectedYear)
        assertEquals(1, viewModel.uiState.value.statistics?.totalNotes)
    }

    private fun note(id: Long, date: LocalDate): Note = Note(
        id = id,
        title = "Note $id",
        content = "Representative content #test",
        creationDate = instant(date)
    )

    private fun instant(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}

private fun Note.toIndexedStatistics(): IndexedNoteStatistics = IndexedNoteStatistics(
    noteId = id,
    creationDate = creationDate,
    sourceLastModifiedDate = lastModifiedDate,
    characterCount = content.length,
    wordCount = 3,
    tagNames = setOf("test")
)

private class StatisticsFakeRepository(
    initialNotes: List<IndexedNoteStatistics>
) : NoteStatisticsRepository {
    private val notes = MutableStateFlow(initialNotes)
    private val state = MutableStateFlow(
        NoteStatisticsIndexState(initialNotes.size, initialNotes.size)
    )
    override val indexedNotes: Flow<List<IndexedNoteStatistics>> = notes
    override val indexState: Flow<NoteStatisticsIndexState> = state

    fun setNotes(values: List<IndexedNoteStatistics>) {
        notes.value = values
    }

    fun setIndexState(indexedNotes: Int, totalNotes: Int) {
        state.value = NoteStatisticsIndexState(indexedNotes, totalNotes)
    }

    override suspend fun rebuildIndex() = Unit
}

private class StatisticsFakeNoteRepository(initialNotes: List<Note>) : NoteRepository {
    private val notes = MutableStateFlow(initialNotes)

    override val allNotes: Flow<List<Note>> = notes
    override val allNotesSortedAsc: Flow<List<Note>> = notes
    override val deletedNotes: Flow<List<Note>> = flowOf(emptyList())
    override val noteLinkCandidates: Flow<List<NoteLinkCandidate>> = flowOf(emptyList())
    override val distinctActiveDays: Flow<Set<Long>> = flowOf(emptySet())
    override val distinctLocalDays: Flow<Set<Long>> = flowOf(emptySet())

    override fun searchNotes(query: String): Flow<List<Note>> = notes
    override fun searchNotesScored(query: String): Flow<List<ScoredNote>> = flowOf(emptyList())
    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>> = notes
    override suspend fun getNoteById(id: Long): Note? = notes.value.firstOrNull { it.id == id }
    override suspend fun saveNote(note: Note): Long = note.id
    override suspend fun moveToTrash(id: Long) = Unit
    override suspend fun restoreFromTrash(id: Long) = Unit
    override suspend fun deleteNotePermanently(id: Long) = Unit
    override suspend fun emptyTrash() = Unit
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
}
