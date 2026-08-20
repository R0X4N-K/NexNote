package io.github.r0x4nk.nexnote.ui.screen.vault

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.usecase.DeleteVaultNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveVaultNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultTrashedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreVaultNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleVaultNotePinUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
    private lateinit var fakeTemplateRepo: FakeTemplateRepository
    private lateinit var fakePreferencesRepo: FakeUserPreferencesRepository
    private lateinit var viewModel: VaultNotesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeVaultRepo = FakeVaultStateRepository()
        fakeNotesRepo = FakeVaultNoteRepository()
        fakeTemplateRepo = FakeTemplateRepository()
        fakePreferencesRepo = FakeUserPreferencesRepository()
        viewModel = VaultNotesViewModel(
            observeVaultState = ObserveVaultStateUseCase(fakeVaultRepo),
            observeVaultNotes = ObserveVaultNotesUseCase(fakeNotesRepo),
            observeVaultTrashedNotes = ObserveVaultTrashedNotesUseCase(fakeNotesRepo),
            moveNoteToVault = MoveNoteToVaultUseCase(fakeNotesRepo),
            moveVaultNoteToTrash = MoveVaultNoteToTrashUseCase(fakeNotesRepo),
            restoreVaultNoteFromTrash = RestoreVaultNoteFromTrashUseCase(fakeNotesRepo),
            deleteVaultNotePermanently = DeleteVaultNotePermanentlyUseCase(fakeNotesRepo),
            toggleVaultNotePin = ToggleVaultNotePinUseCase(fakeNotesRepo),
            duplicateVaultNote = DuplicateVaultNoteUseCase(fakeNotesRepo),
            removeNoteFromVault = RemoveNoteFromVaultUseCase(fakeNotesRepo),
            observeTemplates = ObserveTemplatesUseCase(fakeTemplateRepo),
            observeNoteCardStyle = ObserveNoteCardStyleUseCase(fakePreferencesRepo)
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
        assertFalse(viewModel.uiState.value.isTrashVisible)
    }

    @Test
    fun `note card style preference is exposed without exposing locked notes`() =
        runViewModelTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.noteCardStyle.collect {}
            }
            advanceUntilIdle()

            assertEquals(NoteCardStyle.TITLE_AND_PREVIEW, viewModel.noteCardStyle.value)
            assertFalse(viewModel.uiState.value.isUnlocked)
            assertTrue(viewModel.uiState.value.notes.isEmpty())

            fakePreferencesRepo.setNoteCardStyle(NoteCardStyle.TITLE_ONLY)
            advanceUntilIdle()

            assertEquals(NoteCardStyle.TITLE_ONLY, viewModel.noteCardStyle.value)
            assertFalse(viewModel.uiState.value.isUnlocked)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
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
    fun `trashed notes are exposed only in trash mode while vault is unlocked`() =
        runViewModelTest {
            val active = noteFixture(id = 1L, title = "Active")
            val trashed = noteFixture(id = 2L, title = "Trashed", isDeleted = true)
            fakeNotesRepo.emit(listOf(active))
            fakeNotesRepo.emitTrashed(listOf(trashed))
            advanceUntilIdle()

            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTrashVisible)
            assertTrue(viewModel.uiState.value.notes.isEmpty())

            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTrashVisible)
            assertEquals(listOf(active), viewModel.uiState.value.notes)

            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isTrashVisible)
            assertEquals(listOf(trashed), viewModel.uiState.value.notes)
            assertEquals(1, viewModel.uiState.value.totalNoteCount)

            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTrashVisible)
            assertEquals(listOf(active), viewModel.uiState.value.notes)
        }

    @Test
    fun `trash mode clears search and hides trashed notes when vault locks`() =
        runViewModelTest {
            val active = noteFixture(id = 1L, title = "Active")
            val trashed = noteFixture(id = 2L, title = "Trashed", isDeleted = true)
            fakeNotesRepo.emit(listOf(active))
            fakeNotesRepo.emitTrashed(listOf(trashed))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.toggleTrashVisibility()
            viewModel.onSearchToggle(true)
            viewModel.onSearchQueryChange("trash")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isTrashVisible)
            assertTrue(viewModel.uiState.value.isSearchActive)
            assertEquals(listOf(trashed), viewModel.uiState.value.notes)

            fakeVaultRepo.setState(VaultState.LOCKED)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUnlocked)
            assertFalse(viewModel.uiState.value.isTrashVisible)
            assertFalse(viewModel.uiState.value.isSearchActive)
            assertEquals("", viewModel.uiState.value.searchQuery)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
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

            viewModel.toggleViewMode()
            advanceUntilIdle()

            assertEquals(NoteListViewMode.TAGS, viewModel.uiState.value.viewMode)
            assertEquals(listOf(secret), viewModel.uiState.value.notes)

            fakeVaultRepo.setState(VaultState.LOCKED)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUnlocked)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(NoteListViewMode.TAGS, viewModel.uiState.value.viewMode)
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
    fun `default ui state mirrors home and reports loading until the chain settles`() {
        // The default StateFlow value is what the UI sees before the upstream
        // flow chain has produced any emission. It must mirror Home's
        // `HomeUiState.isLoading = true` so the Vault never flashes a "No
        // Vault notes" empty state before the access state is even known.
        assertTrue(VaultNotesUiState().isLoading)
        assertFalse(VaultNotesUiState().isUnlocked)
        assertTrue(VaultNotesUiState().notes.isEmpty())
    }

    @Test
    fun `locked vault reports not loading once the chain settles`() = runViewModelTest {
        // The unlock/setup form is in charge of the visible state while the
        // Vault is locked. The notes UI must report isLoading=false so the
        // collection branch never accidentally shows a spinner outside the
        // unlocked transition window.
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `unlocked vault reports not loading once the encrypted notes flow has emitted`() =
        runViewModelTest {
            val secret = noteFixture(id = 1L, title = "Secret")
            fakeNotesRepo.emit(listOf(secret))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isUnlocked)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(listOf(secret), viewModel.uiState.value.notes)

            // Vault transitions back to locked: the notes UI must drop the
            // loaded notes and report not-loading because the unlock/setup
            // form is now in charge of the visible state.
            fakeVaultRepo.setState(VaultState.LOCKED)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUnlocked)
            assertFalse(viewModel.uiState.value.isLoading)
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
    fun `search exposes scored ranges and ranks unlocked vault notes like home search`() =
        runViewModelTest {
            val titlePrefix = noteFixture(
                id = 1L,
                title = "Alpha plan",
                content = "private body",
                isPinned = false
            )
            val repeatedContent = noteFixture(
                id = 2L,
                title = "Body",
                content = "alpha and alpha again",
                isPinned = false
            )
            val pinnedContent = noteFixture(
                id = 3L,
                title = "Pinned",
                content = "single alpha",
                isPinned = true
            )
            fakeNotesRepo.emit(listOf(repeatedContent, titlePrefix, pinnedContent))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.onSearchToggle(true)
            viewModel.onSearchQueryChange("alpha")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(3L, 1L, 2L), state.notes.map { it.id })
            assertEquals(listOf(3L, 1L, 2L), state.scoredResults.map { it.note.id })
            assertEquals(listOf(7..11), state.scoredResults[0].contentRanges)
            assertEquals(listOf(0..4), state.scoredResults[1].titleRanges)
            assertTrue(state.scoredResults[1].contentRanges.isEmpty())
            assertEquals(listOf(0..4, 10..14), state.scoredResults[2].contentRanges)

            viewModel.onSearchToggle(false)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.scoredResults.isEmpty())
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
    fun `vault tag filters are exposed only while unlocked`() = runViewModelTest {
        fakeNotesRepo.emit(
            listOf(
                noteFixture(id = 1L, title = "Alpha", content = "#alpha"),
                noteFixture(id = 2L, title = "Beta", content = "#beta")
            )
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnlocked)
        assertTrue(viewModel.uiState.value.topTags.isEmpty())
        assertTrue(viewModel.uiState.value.selectedTagFilters.isEmpty())

        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        assertEquals(listOf("alpha", "beta"), viewModel.uiState.value.topTags.map { it.name })

        viewModel.toggleTagFilter("alpha")
        advanceUntilIdle()

        assertEquals(setOf("alpha"), viewModel.uiState.value.selectedTagFilters)

        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnlocked)
        assertTrue(viewModel.uiState.value.topTags.isEmpty())
        assertTrue(viewModel.uiState.value.selectedTagFilters.isEmpty())
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `tag filters narrow active vault notes in memory`() = runViewModelTest {
        val alphaOnly = noteFixture(id = 1L, title = "Alpha", content = "#alpha")
        val alphaBeta = noteFixture(id = 2L, title = "Both", content = "#alpha #beta")
        val betaOnly = noteFixture(id = 3L, title = "Beta", content = "#beta")
        fakeNotesRepo.emit(listOf(alphaOnly, alphaBeta, betaOnly))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.toggleTagFilter("alpha")
        advanceUntilIdle()

        assertEquals(setOf("alpha"), viewModel.uiState.value.selectedTagFilters)
        assertEquals(listOf(alphaOnly, alphaBeta), viewModel.uiState.value.notes)

        viewModel.toggleTagFilter("beta")
        advanceUntilIdle()

        assertEquals(setOf("alpha", "beta"), viewModel.uiState.value.selectedTagFilters)
        assertEquals(listOf(alphaBeta), viewModel.uiState.value.notes)
    }

    @Test
    fun `vault top tags follow active note content updates from same emission`() =
        runViewModelTest {
            val tagged = noteFixture(id = 1L, title = "Tagged", content = "Body #alpha")
            fakeNotesRepo.emit(listOf(tagged))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            assertEquals(listOf("alpha"), viewModel.uiState.value.topTags.map { it.name })

            fakeNotesRepo.emit(listOf(tagged.copy(content = "Body #beta")))
            advanceUntilIdle()

            assertEquals(listOf("beta"), viewModel.uiState.value.topTags.map { it.name })
            assertEquals(listOf("Body #beta"), viewModel.uiState.value.notes.map { it.content })
        }

    @Test
    fun `vault top tags use the same display limit as normal notes`() = runViewModelTest {
        val notes = (1..(TagRepository.DEFAULT_TOP_TAGS_LIMIT + 2)).map { index ->
            val suffix = index.toString().padStart(2, '0')
            noteFixture(
                id = index.toLong(),
                title = "Tag $suffix",
                content = "Body #tag$suffix"
            )
        }
        fakeNotesRepo.emit(notes)
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        val topTagNames = viewModel.uiState.value.topTags.map { it.name }

        assertEquals(TagRepository.DEFAULT_TOP_TAGS_LIMIT, topTagNames.size)
        assertEquals(
            (1..TagRepository.DEFAULT_TOP_TAGS_LIMIT).map { index ->
                "tag${index.toString().padStart(2, '0')}"
            },
            topTagNames
        )
    }

    @Test
    fun `tag filter remains active when last matching vault tag is removed`() =
        runViewModelTest {
            val tagged = noteFixture(id = 1L, title = "Tagged", content = "Body #alpha")
            val untagged = noteFixture(id = 2L, title = "Untagged", content = "Body")
            fakeNotesRepo.emit(listOf(tagged, untagged))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.toggleTagFilter("alpha")
            advanceUntilIdle()

            assertEquals(setOf("alpha"), viewModel.uiState.value.selectedTagFilters)
            assertEquals(listOf(tagged), viewModel.uiState.value.notes)

            fakeNotesRepo.emit(
                listOf(
                    tagged.copy(content = "Body without tag"),
                    untagged
                )
            )
            advanceUntilIdle()

            assertEquals(setOf("alpha"), viewModel.uiState.value.selectedTagFilters)
            assertTrue(viewModel.uiState.value.topTags.isEmpty())
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(2, viewModel.uiState.value.totalNoteCount)
        }

    @Test
    fun `tag filters ignore title-only hashtags like normal note tags`() = runViewModelTest {
        val titleOnly = noteFixture(
            id = 1L,
            title = "Title #alpha",
            content = "plain body"
        )
        val contentMatch = noteFixture(
            id = 2L,
            title = "No title tag",
            content = "Body with #alpha"
        )
        fakeNotesRepo.emit(listOf(titleOnly, contentMatch))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.toggleTagFilter("alpha")
        advanceUntilIdle()

        assertEquals(setOf("alpha"), viewModel.uiState.value.selectedTagFilters)
        assertEquals(listOf(contentMatch), viewModel.uiState.value.notes)
    }

    @Test
    fun `tag filters are cleared when switching to vault trash`() = runViewModelTest {
        val active = noteFixture(id = 1L, title = "Active", content = "#alpha")
        val trashed = noteFixture(id = 2L, title = "Trashed", content = "#alpha", isDeleted = true)
        fakeNotesRepo.emit(listOf(active))
        fakeNotesRepo.emitTrashed(listOf(trashed))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.toggleTagFilter("alpha")
        advanceUntilIdle()

        assertEquals(setOf("alpha"), viewModel.uiState.value.selectedTagFilters)

        viewModel.toggleTrashVisibility()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTrashVisible)
        assertTrue(viewModel.uiState.value.selectedTagFilters.isEmpty())
        assertTrue(viewModel.uiState.value.topTags.isEmpty())
        assertEquals(listOf(trashed), viewModel.uiState.value.notes)
    }

    @Test
    fun `template picker exposes templates only while unlocked active vault list is visible`() =
        runViewModelTest {
            fakeTemplateRepo.emit(listOf(templateFixture(id = 7L, name = "Daily")))
            advanceUntilIdle()

            viewModel.showTemplatePicker()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showTemplatePicker)
            assertTrue(viewModel.uiState.value.templates.isEmpty())

            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.showTemplatePicker()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showTemplatePicker)
            assertEquals(listOf("Daily"), viewModel.uiState.value.templates.map { it.name })

            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isTrashVisible)
            assertFalse(viewModel.uiState.value.showTemplatePicker)
            assertTrue(viewModel.uiState.value.templates.isEmpty())
        }

    @Test
    fun `template picker is dismissed and cleared when vault locks`() = runViewModelTest {
        fakeTemplateRepo.emit(listOf(templateFixture(id = 7L, name = "Daily")))
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.showTemplatePicker()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showTemplatePicker)

        fakeVaultRepo.setState(VaultState.LOCKED)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnlocked)
        assertFalse(viewModel.uiState.value.showTemplatePicker)
        assertTrue(viewModel.uiState.value.templates.isEmpty())
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
    fun `move to trash emits an undo event without exposing note label`() =
        runViewModelTest {
            val secret = noteFixture(id = 1L, title = "Secret")
            val messages = mutableListOf<String>()
            val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultTrashEvents.collect { trashEvents += it }
            }
            fakeNotesRepo.emit(listOf(secret))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.moveToTrash(secret)
            advanceUntilIdle()

            assertEquals(listOf(1L), fakeNotesRepo.trashedIds)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            // Only the note id and event kind are emitted: the snackbar UI
            // will render a fixed, non-sensitive message, so the title or
            // content cannot leak through here.
            assertEquals(listOf(VaultTrashSnackbarEvent.MovedToTrash(1L)), trashEvents)
            // Success no longer emits a message on vaultActionMessages: that
            // channel is reserved for failure/feedback strings.
            assertTrue(messages.isEmpty())
        }

    @Test
    fun `move to trash emits a non-sensitive failure when use case rejects`() =
        runViewModelTest {
            // Pass an id that the repository does not contain so the use case
            // returns false; the user-facing message must stay generic and
            // must not include any note metadata.
            val unknown = noteFixture(id = 9_999L, title = "Unknown")
            val messages = mutableListOf<String>()
            val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultTrashEvents.collect { trashEvents += it }
            }
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.moveToTrash(unknown)
            advanceUntilIdle()

            assertEquals(listOf("Could not move note to trash"), messages)
            assertTrue(trashEvents.isEmpty())
        }

    @Test
    fun `move to trash is ignored while vault is locked`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        val messages = mutableListOf<String>()
        val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultTrashEvents.collect { trashEvents += it }
        }
        fakeNotesRepo.emit(listOf(secret))
        advanceUntilIdle()

        viewModel.moveToTrash(secret)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.trashedIds.isEmpty())
        assertTrue(messages.isEmpty())
        assertTrue(trashEvents.isEmpty())
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `undo move to trash restores trashed vault note via use case`() =
        runViewModelTest {
            val secret = noteFixture(id = 1L, title = "Secret")
            val messages = mutableListOf<String>()
            val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultTrashEvents.collect { trashEvents += it }
            }
            fakeNotesRepo.emit(listOf(secret))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.moveToTrash(secret)
            advanceUntilIdle()
            assertEquals(listOf(VaultTrashSnackbarEvent.MovedToTrash(1L)), trashEvents)
            assertTrue(viewModel.uiState.value.notes.isEmpty())

            viewModel.undoMoveToTrash(secret.id)
            advanceUntilIdle()

            assertEquals(listOf(1L), fakeNotesRepo.restoredIds)
            assertEquals(
                listOf(secret.copy(isDeleted = false, deletedDate = null)),
                viewModel.uiState.value.notes
            )
            // The success path emits neither an extra message nor a duplicate
            // trash event when undoing.
            assertTrue(messages.isEmpty())
            assertEquals(listOf(VaultTrashSnackbarEvent.MovedToTrash(1L)), trashEvents)
        }

    @Test
    fun `undo move to trash is ignored while vault is locked`() = runViewModelTest {
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        advanceUntilIdle()

        viewModel.undoMoveToTrash(42L)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.restoredIds.isEmpty())
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `undo move to trash ignores invalid ids`() = runViewModelTest {
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeVaultRepo.setState(VaultState.UNLOCKED)
        advanceUntilIdle()

        viewModel.undoMoveToTrash(0L)
        viewModel.undoMoveToTrash(-1L)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.restoredIds.isEmpty())
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `toggle pin updates unlocked active vault note through vault save path`() =
        runViewModelTest {
            val secret = noteFixture(id = 1L, title = "Secret", isPinned = false)
            val messages = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            fakeNotesRepo.emit(listOf(secret))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.togglePin(secret)
            advanceUntilIdle()

            assertEquals(listOf(secret.copy(isPinned = true)), fakeNotesRepo.savedNotes)
            assertTrue(viewModel.uiState.value.notes.single().isPinned)
            assertTrue(messages.isEmpty())
        }

    @Test
    fun `toggle pin is ignored while vault is locked`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeNotesRepo.emit(listOf(secret))
        advanceUntilIdle()

        viewModel.togglePin(secret)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.savedNotes.isEmpty())
        assertTrue(messages.isEmpty())
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `duplicate duplicates unlocked active vault note through vault path`() =
        runViewModelTest {
            val secret = noteFixture(id = 1L, title = "Secret")
            val messages = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            fakeNotesRepo.emit(listOf(secret))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.duplicate(secret)
            advanceUntilIdle()

            assertEquals(listOf(1L), fakeNotesRepo.duplicatedIds)
            assertEquals(listOf("Vault note duplicated"), messages)
            assertEquals(listOf(1L, 2L), viewModel.uiState.value.notes.map { it.id })
        }

    @Test
    fun `duplicate is ignored while vault is locked`() = runViewModelTest {
        val secret = noteFixture(id = 1L, title = "Secret")
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeNotesRepo.emit(listOf(secret))
        advanceUntilIdle()

        viewModel.duplicate(secret)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.duplicatedIds.isEmpty())
        assertTrue(messages.isEmpty())
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `restore from trash moves deleted vault note back through use case`() =
        runViewModelTest {
            val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
            val messages = mutableListOf<String>()
            val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultTrashEvents.collect { trashEvents += it }
            }
            fakeNotesRepo.emitTrashed(listOf(deleted))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            viewModel.restoreFromTrash(deleted)
            advanceUntilIdle()

            assertEquals(listOf(2L), fakeNotesRepo.restoredIds)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertTrue(messages.isEmpty())
            assertEquals(listOf(VaultTrashSnackbarEvent.RestoredFromTrash(2L)), trashEvents)

            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            assertEquals(
                listOf(deleted.copy(isDeleted = false, deletedDate = null)),
                viewModel.uiState.value.notes
            )
        }

    @Test
    fun `restore from trash is ignored outside vault trash mode`() =
        runViewModelTest {
            val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
            val messages = mutableListOf<String>()
            val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultTrashEvents.collect { trashEvents += it }
            }
            fakeNotesRepo.emitTrashed(listOf(deleted))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.restoreFromTrash(deleted)
            advanceUntilIdle()

            assertTrue(fakeNotesRepo.restoredIds.isEmpty())
            assertTrue(messages.isEmpty())
            assertTrue(trashEvents.isEmpty())
        }

    @Test
    fun `undo restore from trash moves restored note back to vault trash`() =
        runViewModelTest {
            val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
            val messages = mutableListOf<String>()
            val trashEvents = mutableListOf<VaultTrashSnackbarEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultTrashEvents.collect { trashEvents += it }
            }
            fakeNotesRepo.emitTrashed(listOf(deleted))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            viewModel.restoreFromTrash(deleted)
            advanceUntilIdle()
            viewModel.undoTrashSnackbarEvent(VaultTrashSnackbarEvent.RestoredFromTrash(deleted.id))
            advanceUntilIdle()

            assertEquals(listOf(2L), fakeNotesRepo.restoredIds)
            assertEquals(listOf(2L), fakeNotesRepo.trashedIds)
            assertEquals(listOf(deleted.copy(isDeleted = true)), viewModel.uiState.value.notes)
            assertEquals(listOf(VaultTrashSnackbarEvent.RestoredFromTrash(2L)), trashEvents)
            assertTrue(messages.isEmpty())
        }

    @Test
    fun `undo restore from trash is ignored outside vault trash mode`() =
        runViewModelTest {
            val messages = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()

            viewModel.undoRestoreFromTrash(2L)
            advanceUntilIdle()

            assertTrue(fakeNotesRepo.trashedIds.isEmpty())
            assertTrue(messages.isEmpty())
        }

    @Test
    fun `restore from trash is ignored while vault is locked`() = runViewModelTest {
        val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeNotesRepo.emitTrashed(listOf(deleted))
        advanceUntilIdle()

        viewModel.restoreFromTrash(deleted)
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.restoredIds.isEmpty())
        assertTrue(messages.isEmpty())
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `request delete permanently is limited to deleted vault notes while unlocked`() =
        runViewModelTest {
            val active = noteFixture(id = 1L, title = "Active")
            val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
            fakeNotesRepo.emit(listOf(active))
            fakeNotesRepo.emitTrashed(listOf(deleted))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            viewModel.requestDeletePermanentlyFromTrash(active)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.notePendingPermanentDeleteId)

            viewModel.requestDeletePermanentlyFromTrash(deleted)
            advanceUntilIdle()

            assertEquals(2L, viewModel.uiState.value.notePendingPermanentDeleteId)

            viewModel.cancelDeletePermanentlyFromTrash()
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.notePendingPermanentDeleteId)
        }

    @Test
    fun `confirm delete permanently removes deleted vault note through use case`() =
        runViewModelTest {
            val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
            val messages = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            fakeNotesRepo.emitTrashed(listOf(deleted))
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            advanceUntilIdle()
            viewModel.toggleTrashVisibility()
            advanceUntilIdle()

            viewModel.requestDeletePermanentlyFromTrash(deleted)
            advanceUntilIdle()
            viewModel.confirmDeletePermanentlyFromTrash()
            advanceUntilIdle()

            assertEquals(listOf(2L), fakeNotesRepo.permanentlyDeletedIds)
            assertEquals(null, viewModel.uiState.value.notePendingPermanentDeleteId)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(listOf("Note permanently deleted"), messages)
        }

    @Test
    fun `delete permanently is ignored while vault is locked`() = runViewModelTest {
        val deleted = noteFixture(id = 2L, title = "Deleted", isDeleted = true)
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.vaultActionMessages.collect { messages += it }
        }
        fakeNotesRepo.emitTrashed(listOf(deleted))
        advanceUntilIdle()

        viewModel.requestDeletePermanentlyFromTrash(deleted)
        viewModel.confirmDeletePermanentlyFromTrash()
        advanceUntilIdle()

        assertTrue(fakeNotesRepo.permanentlyDeletedIds.isEmpty())
        assertEquals(null, viewModel.uiState.value.notePendingPermanentDeleteId)
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
    fun `not found move result shows generic failure without exposing content`() =
        runViewModelTest {
            val messages = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.vaultActionMessages.collect { messages += it }
            }
            fakeVaultRepo.setState(VaultState.UNLOCKED)
            fakeNotesRepo.nextMoveResult = MoveNoteToVaultResult.NotFound
            advanceUntilIdle()

            viewModel.moveNormalNoteToVault(10L)
            advanceUntilIdle()

            assertEquals(listOf(10L), fakeNotesRepo.movedIds)
            assertTrue(viewModel.uiState.value.notes.isEmpty())
            assertEquals(listOf("Could not move note to Vault"), messages)
        }

    private fun noteFixture(
        id: Long,
        title: String,
        content: String = "body of $title",
        lastModifiedDate: Long = 0L,
        isPinned: Boolean = false,
        isDeleted: Boolean = false
    ): Note = Note(
        id = id,
        title = title,
        content = content,
        lastModifiedDate = lastModifiedDate,
        isInVault = true,
        isPinned = isPinned,
        isDeleted = isDeleted
    )

    private fun templateFixture(id: Long, name: String): Template = Template(
        id = id,
        name = name,
        content = "Template body",
        isMarkdown = true
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
    private val trashedNotesFlow = MutableStateFlow<List<Note>>(emptyList())
    override val vaultNotes: Flow<List<Note>> = notesFlow
    override val vaultTrashedNotes: Flow<List<Note>> = trashedNotesFlow
    override val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>> =
        notesFlow.map { notes ->
            notes.filter { it.isInVault && !it.isDeleted }
                .map { note -> NoteLinkCandidate(id = note.id, title = note.title) }
        }
    val removedIds = mutableListOf<Long>()
    val movedIds = mutableListOf<Long>()
    val trashedIds = mutableListOf<Long>()
    val restoredIds = mutableListOf<Long>()
    val permanentlyDeletedIds = mutableListOf<Long>()
    val duplicatedIds = mutableListOf<Long>()
    val savedNotes = mutableListOf<Note>()
    var nextMoveResult: MoveNoteToVaultResult = MoveNoteToVaultResult.Success
    var nextDuplicateResult: DuplicateVaultNoteResult? = null

    fun emit(notes: List<Note>) {
        notesFlow.value = notes
    }

    fun emitTrashed(notes: List<Note>) {
        trashedNotesFlow.value = notes
    }

    override suspend fun getVaultNoteById(id: Long): Note? =
        notesFlow.value.firstOrNull { it.id == id }

    override suspend fun saveVaultNote(note: Note): Long {
        val saved = note.copy(isInVault = true)
        savedNotes += saved
        notesFlow.value = notesFlow.value
            .map { current -> if (current.id == saved.id) saved else current }
        return saved.id
    }

    override suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult {
        duplicatedIds += id
        nextDuplicateResult?.let { return it }
        val source = notesFlow.value.firstOrNull { it.id == id && it.isInVault && !it.isDeleted }
            ?: return DuplicateVaultNoteResult.NotFound
        val duplicateId = (notesFlow.value + trashedNotesFlow.value)
            .maxOfOrNull { it.id }
            ?.plus(1L)
            ?: 1L
        notesFlow.value = notesFlow.value + source.copy(id = duplicateId)
        return DuplicateVaultNoteResult.Success(duplicateId)
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

    override suspend fun moveVaultNoteToTrash(id: Long): Boolean {
        trashedIds += id
        val note = notesFlow.value.firstOrNull { it.id == id } ?: return false
        notesFlow.value = notesFlow.value.filterNot { it.id == note.id }
        trashedNotesFlow.value = trashedNotesFlow.value + note.copy(isDeleted = true)
        return true
    }

    override suspend fun restoreVaultNoteFromTrash(id: Long): Boolean {
        restoredIds += id
        val note = trashedNotesFlow.value.firstOrNull { it.id == id } ?: return false
        trashedNotesFlow.value = trashedNotesFlow.value.filterNot { it.id == note.id }
        notesFlow.value = notesFlow.value + note.copy(isDeleted = false, deletedDate = null)
        return true
    }

    override suspend fun deleteVaultNotePermanently(id: Long): Boolean {
        permanentlyDeletedIds += id
        val note = trashedNotesFlow.value.firstOrNull { it.id == id } ?: return false
        trashedNotesFlow.value = trashedNotesFlow.value.filterNot { it.id == note.id }
        return true
    }

    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? = null
}

private class FakeTemplateRepository : TemplateRepository {
    private val templatesFlow = MutableStateFlow<List<Template>>(emptyList())
    override val allTemplates: Flow<List<Template>> = templatesFlow

    fun emit(templates: List<Template>) {
        templatesFlow.value = templates
    }

    override suspend fun getTemplateById(id: Long): Template? =
        templatesFlow.value.firstOrNull { it.id == id }

    override suspend fun saveTemplate(template: Template): Long {
        throw UnsupportedOperationException("Not needed for Vault notes ViewModel tests")
    }

    override suspend fun deleteTemplate(template: Template) {
        throw UnsupportedOperationException("Not needed for Vault notes ViewModel tests")
    }
}

private class FakeUserPreferencesRepository : IUserPreferencesRepository {
    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
    override val fontScale: Flow<FontScale> = MutableStateFlow(FontScale.NORMAL)
    override val timezoneId: Flow<String> = MutableStateFlow("UTC")
    override val accentColor: Flow<AccentColor> = MutableStateFlow(AccentColor.VIOLET)
    private val noteCardStyleFlow = MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)
    override val noteCardStyle: Flow<NoteCardStyle> = noteCardStyleFlow
    override val tableLayoutMode: Flow<TableLayoutMode> =
        MutableStateFlow(TableLayoutMode.FIT_SCREEN)
    override val protectVaultRecentPreviews: Flow<Boolean> = MutableStateFlow(true)
    override val lockVaultOnBackground: Flow<Boolean> = MutableStateFlow(true)
    override val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout> =
        MutableStateFlow(VaultAutoLockTimeout.IMMEDIATELY)
    override val unlockVaultWithAndroidCredential: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
    override suspend fun setFontScale(scale: FontScale) = Unit
    override suspend fun setTimezoneId(id: String) = Unit
    override suspend fun setAccentColor(color: AccentColor) = Unit

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        noteCardStyleFlow.value = style
    }

    override suspend fun setTableLayoutMode(mode: TableLayoutMode) = Unit

    override suspend fun setProtectVaultRecentPreviews(value: Boolean) = Unit
    override suspend fun setLockVaultOnBackground(value: Boolean) = Unit
    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) = Unit
    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) = Unit
}
