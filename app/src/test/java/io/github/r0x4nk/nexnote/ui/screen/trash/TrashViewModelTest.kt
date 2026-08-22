package io.github.r0x4nk.nexnote.ui.screen.trash

import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.EmptyTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val events = mutableListOf<String>()
    private lateinit var fakeDao: FakeNoteDao
    private lateinit var fakeImageStorage: FakeNoteImageStorage
    private lateinit var viewModel: TrashViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        events.clear()
        fakeDao = FakeNoteDao(events)
        fakeImageStorage = FakeNoteImageStorage(events)
        val repository = NoteRepositoryImpl(fakeDao, fakeImageStorage)
        viewModel = TrashViewModel(
            observeDeletedNotes = ObserveDeletedNotesUseCase(repository),
            restoreNoteFromTrash = RestoreNoteFromTrashUseCase(repository),
            deleteNotePermanently = DeleteNotePermanentlyUseCase(repository),
            emptyTrash = EmptyTrashUseCase(repository)
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

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state - noteToDelete is null and dialog hidden`() = runViewModelTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNull(state.noteToDelete)
        assertFalse(state.showEmptyTrashDialog)
    }

    // ── requestDeletePermanently / cancelDelete ────────────────────────────────

    @Test
    fun `requestDeletePermanently sets noteToDelete`() = runViewModelTest {
        val note = deletedNote(id = 1L, title = "Test note")
        viewModel.requestDeletePermanently(note)
        advanceUntilIdle()
        assertEquals(note, viewModel.uiState.value.noteToDelete)
    }

    @Test
    fun `cancelDelete clears noteToDelete`() = runViewModelTest {
        viewModel.requestDeletePermanently(deletedNote(id = 1L))
        viewModel.cancelDelete()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.noteToDelete)
    }

    @Test
    fun `confirmDeletePermanently calls dao and clears noteToDelete`() = runViewModelTest {
        val note = deletedNote(id = 2L)
        fakeDao.emitDeletedNotes(listOf(deletedNoteEntity(2L, "Note", 1000L, "")))
        viewModel.requestDeletePermanently(note)
        viewModel.confirmDeletePermanently()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.noteToDelete)
        assertEquals(1, fakeDao.permanentlyDeletedCount)
    }

    @Test
    fun `confirmDeletePermanently is no-op when noteToDelete is null`() = runViewModelTest {
        viewModel.confirmDeletePermanently()
        advanceUntilIdle()
        assertEquals(0, fakeDao.permanentlyDeletedCount)
    }

    @Test
    fun `confirmDeletePermanently deletes image files after dao delete`() = runViewModelTest {
        val note = deletedNote(id = 2L, imagePaths = listOf("images/a.jpg", "images/b.jpg"))
        fakeDao.emitDeletedNotes(
            listOf(deletedNoteEntity(2L, "Note", 1000L, "images/a.jpg\nimages/b.jpg"))
        )
        viewModel.requestDeletePermanently(note)

        viewModel.confirmDeletePermanently()
        advanceUntilIdle()

        assertEquals(listOf("images/a.jpg", "images/b.jpg"), fakeImageStorage.deletedPaths)
        assertEquals(
            listOf("image:images/a.jpg", "image:images/b.jpg", "dao:delete:2"),
            events
        )
    }

    @Test
    fun `permanent deletion retries transient image failure before deleting row`() =
        runViewModelTest {
            val note = deletedNote(id = 3L, imagePaths = listOf("images/a.jpg"))
            fakeDao.emitDeletedNotes(
                listOf(deletedNoteEntity(3L, "Note", 1000L, "images/a.jpg"))
            )
            fakeImageStorage.deleteFailuresRemaining = 2
            viewModel.requestDeletePermanently(note)

            viewModel.confirmDeletePermanently()
            advanceUntilIdle()

            assertEquals(3, fakeImageStorage.deletedPaths.size)
            assertEquals(1, fakeDao.permanentlyDeletedCount)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `persistent image failure keeps row and dialog available for retry`() =
        runViewModelTest {
            val note = deletedNote(id = 4L, imagePaths = listOf("images/a.jpg"))
            fakeDao.emitDeletedNotes(
                listOf(deletedNoteEntity(4L, "Note", 1000L, "images/a.jpg"))
            )
            fakeImageStorage.deleteFailuresRemaining = Int.MAX_VALUE
            viewModel.requestDeletePermanently(note)

            viewModel.confirmDeletePermanently()
            advanceUntilIdle()

            assertEquals(3, fakeImageStorage.deletedPaths.size)
            assertEquals(0, fakeDao.permanentlyDeletedCount)
            assertEquals(note, viewModel.uiState.value.noteToDelete)
            assertTrue(viewModel.uiState.value.errorMessage?.isNotBlank() == true)
        }

    // ── requestEmptyTrash / cancelEmptyTrash ──────────────────────────────────

    @Test
    fun `requestEmptyTrash shows empty trash dialog`() = runViewModelTest {
        viewModel.requestEmptyTrash()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showEmptyTrashDialog)
    }

    @Test
    fun `cancelEmptyTrash hides dialog`() = runViewModelTest {
        viewModel.requestEmptyTrash()
        viewModel.cancelEmptyTrash()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showEmptyTrashDialog)
    }

    @Test
    fun `confirmEmptyTrash calls dao and hides dialog`() = runViewModelTest {
        viewModel.requestEmptyTrash()
        viewModel.confirmEmptyTrash()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showEmptyTrashDialog)
        assertEquals(1, fakeDao.emptyTrashCount)
    }

    @Test
    fun `confirmEmptyTrash deletes trashed image files after dao empty trash`() = runViewModelTest {
        fakeDao.emitDeletedNotes(deletedNotesWithImages())
        advanceUntilIdle()
        viewModel.requestEmptyTrash()

        viewModel.confirmEmptyTrash()
        advanceUntilIdle()

        assertEquals(deletedImagePaths(), fakeImageStorage.deletedPaths)
        assertEquals(deletedImageEvents() + "dao:empty", events)
    }

    // ── restoreNote ───────────────────────────────────────────────────────────

    @Test
    fun `restoreNote calls dao restoreFromTrash`() = runViewModelTest {
        viewModel.restoreNote(noteId = 5L)
        advanceUntilIdle()
        assertEquals(1, fakeDao.restoredCount)
        assertEquals(5L, fakeDao.lastRestoredId)
    }

    // ── notes flow ────────────────────────────────────────────────────────────

    @Test
    fun `notes emitted from dao appear in uiState`() = runViewModelTest {
        val entity = NoteEntity(id = 10L, title = "In trash", isDeleted = true, deletedDate = 1000L)
        fakeDao.emitDeletedNotes(listOf(entity))
        advanceUntilIdle()
        val notes = viewModel.uiState.value.notes
        assertEquals(1, notes.size)
        assertEquals(10L, notes.first().id)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun deletedNote(
        id: Long,
        title: String = "Note",
        imagePaths: List<String> = emptyList()
    ): Note = Note(
        id = id,
        title = title,
        isDeleted = true,
        deletedDate = System.currentTimeMillis(),
        imagePaths = imagePaths
    )

    private fun deletedNotesWithImages(): List<NoteEntity> = listOf(
        deletedNoteEntity(10L, "One", 1000L, "images/a.jpg\nimages/b.jpg"),
        deletedNoteEntity(11L, "Two", 1001L, "images/c.jpg")
    )

    private fun deletedNoteEntity(
        id: Long,
        title: String,
        deletedDate: Long,
        imagePathsRaw: String
    ): NoteEntity = NoteEntity(
        id = id,
        title = title,
        isDeleted = true,
        deletedDate = deletedDate,
        imagePathsRaw = imagePathsRaw
    )

    private fun deletedImagePaths(): List<String> =
        listOf("images/a.jpg", "images/b.jpg", "images/c.jpg")

    private fun deletedImageEvents(): List<String> =
        deletedImagePaths().map { "image:$it" }
}
