package io.github.r0x4nk.nexnote.ui.screen.vault

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultNotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeVaultRepo: FakeVaultStateRepository
    private lateinit var fakeNotesRepo: FakeVaultNoteRepository
    private lateinit var viewModel: VaultNotesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeVaultRepo = FakeVaultStateRepository()
        fakeNotesRepo = FakeVaultNoteRepository()
        viewModel = VaultNotesViewModel(
            observeVaultState = ObserveVaultStateUseCase(fakeVaultRepo),
            observeVaultNotes = ObserveVaultNotesUseCase(fakeNotesRepo),
            moveNoteToVault = MoveNoteToVaultUseCase(fakeNotesRepo),
            removeNoteFromVault = RemoveNoteFromVaultUseCase(fakeNotesRepo)
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
        advanceUntilIdle()
        block()
    }

    @Test
    fun `initial state reports locked and empty notes`() = runViewModelTest {
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(SortOrder.MODIFIED_DESC, viewModel.uiState.value.sortOrder)
        assertEquals(NoteListViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `notes stay empty when vault is locked even if repository emits notes`() =
        runViewModelTest {
            fakeNotesRepo.emit(listOf(noteFixture(id = 1L, title = "Secret")))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUnlocked)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
        }

    @Test
    fun `notes are exposed only after vault becomes unlocked`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        fakeNotesRepo.emit(listOf(secret))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notes.isEmpty())

        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isUnlocked)
        assertEquals(listOf(secret), viewModel.uiState.value.notes)
    }

    @Test
    fun `unlocked vault notes can be sorted by modified date without exposing locked notes`() =
        runViewModelTest {
            val older = noteFixture(id = 1L, title = "Older", lastModifiedDate = 10L)
            val newest = noteFixture(id = 2L, title = "Newest", lastModifiedDate = 30L)
            val pinnedOlder = noteFixture(
                id = 3L,
                title = "Pinned",
                lastModifiedDate = 5L,
                isPinned = true
            )
            fakeNotesRepo.emit(listOf(older, pinnedOlder, newest))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            assertEquals(SortOrder.MODIFIED_DESC, viewModel.uiState.value.sortOrder)
            assertEquals(listOf(pinnedOlder, newest, older), viewModel.uiState.value.notes)

            viewModel.toggleSortOrder()
            advanceUntilIdle()

            assertEquals(SortOrder.MODIFIED_ASC, viewModel.uiState.value.sortOrder)
            assertEquals(listOf(pinnedOlder, older, newest), viewModel.uiState.value.notes)

            fakeVaultRepo.setState(VaultState.LOCKED)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUnlocked)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(SortOrder.MODIFIED_ASC, viewModel.uiState.value.sortOrder)
        }

    @Test
    fun `unlocked vault view mode toggles between list and grid without exposing locked notes`() =
        runViewModelTest {
            val secret = noteFixture(id = 1L, title = "Secret")
            fakeNotesRepo.emit(listOf(secret))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            assertEquals(NoteListViewMode.LIST, viewModel.uiState.value.viewMode)
            assertEquals(listOf(secret), viewModel.uiState.value.notes)

            viewModel.toggleViewMode()
            advanceUntilIdle()

            assertEquals(NoteListViewMode.GRID, viewModel.uiState.value.viewMode)
            assertEquals(listOf(secret), viewModel.uiState.value.notes)

            fakeVaultRepo.setState(VaultState.LOCKED)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUnlocked)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(NoteListViewMode.GRID, viewModel.uiState.value.viewMode)
        }

    @Test
    fun `notes are cleared from UI state when vault locks again`() = runViewModelTest {
        fakeNotesRepo.emit(listOf(noteFixture(id = 1L, title = "Secret")))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notes.isNotEmpty())

        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnlocked)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `search filters unlocked vault notes by title and content only in memory`() =
        runViewModelTest {
            val alpha = noteFixture(id = 1L, title = "Alpha plan", content = "private body")
            val beta = noteFixture(id = 2L, title = "Beta", content = "contains hidden clue")
            val gamma = noteFixture(id = 3L, title = "Gamma", content = "unrelated")
            fakeNotesRepo.emit(listOf(alpha, beta, gamma))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.onSearchToggle(true)
            viewModel.onSearchQueryChange("alpha")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSearchActive)
            assertEquals("alpha", viewModel.uiState.value.searchQuery)
            assertEquals(listOf(alpha), viewModel.uiState.value.notes)
            assertEquals(3, viewModel.uiState.value.totalNoteCount)

            viewModel.onSearchQueryChange("HIDDEN")
            advanceUntilIdle()

            assertEquals(listOf(beta), viewModel.uiState.value.notes)
        }

    @Test
    fun `closing search clears query and restores all unlocked vault notes`() =
        runViewModelTest {
            val alpha = noteFixture(id = 1L, title = "Alpha")
            val beta = noteFixture(id = 2L, title = "Beta")
            fakeNotesRepo.emit(listOf(alpha, beta))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.onSearchToggle(true)
            viewModel.onSearchQueryChange("alpha")
            advanceUntilIdle()
            assertEquals(listOf(alpha), viewModel.uiState.value.notes)

            viewModel.onSearchToggle(false)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSearchActive)
            assertEquals("", viewModel.uiState.value.searchQuery)
            assertEquals(listOf(alpha, beta), viewModel.uiState.value.notes)
        }

    @Test
    fun `search state is cleared when vault locks again`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        fakeNotesRepo.emit(listOf(secret))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()
        viewModel.onSearchToggle(true)
        viewModel.onSearchQueryChange("secret")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSearchActive)

        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnlocked)
        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `remove from vault moves unlocked note out through use case`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeNotesRepo.emit(listOf(secret))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.removeFromVault(secret)
        advanceUntilIdle()

        assertEquals(listOf(1L), fakeNotesRepo.removedIds)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
        assertEquals(listOf("Note removed from Vault"), messages)
    }

    @Test
    fun `remove from vault is ignored while vault is locked`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeNotesRepo.emit(listOf(secret))
        advanceUntilIdle()

        viewModel.removeFromVault(secret)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.removedIds.isEmpty())
        assertTrue(messages.isEmpty())
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `move normal note to vault runs only when vault is unlocked`() = runViewModelTest {
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }

        viewModel.moveNormalNoteToVault(10L)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.movedIds.isEmpty())
        assertTrue(messages.isEmpty())

        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.moveNormalNoteToVault(10L)
        advanceUntilIdle()

        assertEquals(listOf(10L), fakeNotesRepo.movedIds)
        assertEquals(listOf("Note moved to Vault"), messages)
    }

    @Test
    fun `move normal note with images shows unsupported message without exposing content`() =
        runViewModelTest {
            val messages = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            fakeNotesRepo.nextMoveResult = MoveNoteToVaultResult.ContainsImages
            advanceUntilIdle()

            viewModel.moveNormalNoteToVault(10L)
            advanceUntilIdle()

            assertEquals(listOf(10L), fakeNotesRepo.movedIds)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(listOf("Notes with images cannot be moved to Vault yet"), messages)
        }

    private fun noteFixture(
        id: Long,
        title: String,
        content: String = "body of $title",
        lastModifiedDate: Long = 0L,
        isPinned: Boolean = false
    ): Note = Note(
        id = id,
        title = title,
        content = content,
        lastModifiedDate = lastModifiedDate,
        isInVault = true,
        isPinned = isPinned
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private class FakeVaultStateRepository : VaultRepository {
    private val stateFlow = MutableStateFlow(VaultState.LOCKED)
    private val hasProtectedMaterial = MutableStateFlow(false)
    override val state: Flow<VaultState> = stateFlow
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        hasProtectedMaterial

    fun setState(newState: VaultState) {
        stateFlow.value = newState
    }

    override suspend fun configurePin(pin: CharArray) {
        stateFlow.value = VaultState.LOCKED
    }

    override suspend fun unlockWithPin(pin: CharArray): Boolean {
        stateFlow.value = VaultState.UNLOCKED
        return true
    }

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult =
        RefreshVaultAndroidCredentialProtectedMaterialResult.Failed

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() = Unit

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult = ChangeVaultPinResult.RewrapFailed

    override suspend fun resetVault(): ResetVaultResult = ResetVaultResult.Failed

    override fun lock() {
        stateFlow.value = VaultState.LOCKED
    }
}

private class FakeVaultNoteRepository : VaultNoteRepository {
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    override val vaultNotes: Flow<List<Note>> = notesFlow
    val removedIds = mutableListOf<Long>()
    val movedIds = mutableListOf<Long>()
    var nextMoveResult: MoveNoteToVaultResult = MoveNoteToVaultResult.Success

    fun emit(notes: List<Note>) {
        notesFlow.value = notes
    }

    override suspend fun getVaultNoteById(id: Long): Note? =
        notesFlow.value.firstOrNull { it.id == id }

    override suspend fun saveVaultNote(note: Note): Long {
        throw UnsupportedOperationException("Not needed for VaultNotesViewModelTest")
    }

    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult {
        movedIds += id
        if (nextMoveResult != MoveNoteToVaultResult.Success) return nextMoveResult
        notesFlow.value = notesFlow.value + Note(id = id, title = "Moved note", isInVault = true)
        return MoveNoteToVaultResult.Success
    }

    override suspend fun removeNoteFromVault(id: Long): Boolean {
        removedIds += id
        val note = notesFlow.value.firstOrNull { it.id == id } ?: return false
        notesFlow.value = notesFlow.value.filterNot { it.id == note.id }
        return true
    }
}
