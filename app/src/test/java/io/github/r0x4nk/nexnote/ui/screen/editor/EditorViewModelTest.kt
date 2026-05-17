package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest : EditorViewModelTestBase() {

    @Test
    fun `new note has empty state and is not dirty`() = runTest {
        val vm = viewModel()
        runCurrent()
        val state = vm.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertFalse(state.isDirty)
        assertFalse(state.isLoading)
        assertEquals(EditorViewModel.NO_ID, state.noteId)
    }

    @Test
    fun `new note opened with creation date keeps that date when saved`() = runTest {
        val initialCreationDate = 123_456L
        val vm = viewModel(creationDate = initialCreationDate)
        runCurrent()

        assertEquals(initialCreationDate, vm.uiState.value.creationDate)
        assertFalse(vm.uiState.value.isDirty)

        vm.onContentChange("Agenda note")
        vm.flushPendingChanges()
        advanceUntilIdle()

        val savedNote = fakeNoteDao.getNoteById(vm.uiState.value.noteId)
        assertEquals(initialCreationDate, savedNote?.creationDate)
    }

    @Test
    fun `loadNote populates state with note data`() = runTest {
        fakeNoteDao.addNote(NoteEntity(id = 42L, title = "Title", content = "Content"))
        val vm = viewModel(noteId = 42L)
        runCurrent()
        val state = vm.uiState.value
        assertEquals(42L, state.noteId)
        assertEquals("Title", state.title)
        assertEquals("Content", state.content)
        assertFalse(state.isDirty)
        assertFalse(state.isLoading)
        assertFalse(state.openedDirectlyInPreview)
    }

    @Test
    fun `loadNote keeps direct preview intent for warmup`() = runTest {
        fakeNoteDao.addNote(
            NoteEntity(
                id = 42L,
                title = "Title",
                content = "# Long note",
                isPreviewMode = true
            )
        )

        val vm = viewModel(noteId = 42L)
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state.showPreview)
        assertTrue(state.openedDirectlyInPreview)
        assertFalse(state.isLoading)
    }

    @Test
    fun `noteLinkTargets excludes current note and normalizes blank titles`() = runTest {
        fakeNoteDao.addNote(NoteEntity(id = 42L, title = "Current"))
        fakeNoteDao.addNote(NoteEntity(id = 7L, title = "   "))

        val vm = viewModel(noteId = 42L)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.noteLinkTargets.collect {}
        }
        advanceUntilIdle()

        assertEquals(listOf(NoteLinkTarget(id = 7L, title = "Untitled note")), vm.noteLinkTargets.value)
        collectJob.cancel()
    }

    @Test
    fun `loadNote with unknown id sets errorMessage`() = runTest {
        val vm = viewModel(noteId = 999L)
        runCurrent()
        assertTrue(vm.uiState.value.errorMessage?.isNotBlank() == true)
    }

    @Test
    fun `vault note mode loads note through vault repository as editable vault content`() = runTest {
        val vaultRepository = FakeEditorVaultNoteRepository()
        vaultRepository.addNote(
            Note(
                id = 77L,
                title = "Private title",
                content = "Private body",
                isInVault = true
            )
        )

        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository)
        )
        runCurrent()

        val state = vm.uiState.value
        assertEquals(77L, state.noteId)
        assertEquals("Private title", state.title)
        assertEquals("Private body", state.content)
        assertTrue(state.isVaultNote)
        assertFalse(state.isReadOnly)
        assertFalse(state.isDirty)
        assertFalse(state.isLoading)
    }

    @Test
    fun `vault note mode saves edits through vault repository only`() = runTest {
        val vaultRepository = FakeEditorVaultNoteRepository()
        vaultRepository.addNote(
            Note(
                id = 77L,
                title = "Private title",
                content = "Private body",
                isInVault = true
            )
        )
        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onTitleChange("Updated title")
        vm.onContentChange("Updated body")
        vm.onBackgroundColorChange(0x123456)
        vm.flushPendingChanges()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Updated title", state.title)
        assertEquals("Updated body", state.content)
        assertFalse(state.isDirty)
        assertEquals(0, fakeNoteDao.insertedCount)
        assertEquals(0, fakeNoteDao.updatedCount)
        assertEquals(1, vaultRepository.savedNotes.size)
        assertEquals("Updated title", vaultRepository.savedNotes.single().title)
        assertEquals("Updated body", vaultRepository.savedNotes.single().content)
        assertTrue(vaultRepository.savedNotes.single().isInVault)
    }

    @Test
    fun `new vault note mode saves new content through vault repository only`() = runTest {
        val vaultRepository = FakeEditorVaultNoteRepository()
        val vm = viewModel(
            mode = EditorMode.NewVaultNote,
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        assertTrue(vm.uiState.value.isVaultNote)
        assertFalse(vm.uiState.value.isLoading)

        vm.onTitleChange("Private draft")
        vm.onContentChange("Encrypted later by repository")
        vm.flushPendingChanges()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotEquals(EditorViewModel.NO_ID, state.noteId)
        assertFalse(state.isDirty)
        assertEquals(0, fakeNoteDao.insertedCount)
        assertEquals(0, fakeNoteDao.updatedCount)
        assertEquals(1, vaultRepository.savedNotes.size)
        assertEquals("Private draft", vaultRepository.savedNotes.single().title)
        assertEquals("Encrypted later by repository", vaultRepository.savedNotes.single().content)
        assertTrue(vaultRepository.savedNotes.single().isInVault)
    }

    @Test
    fun `vault editor clears decrypted state when vault locks`() = runTest {
        val vaultRepository = FakeEditorVaultNoteRepository()
        val vaultStateRepository = FakeEditorVaultStateRepository(VaultState.UNLOCKED)
        vaultRepository.addNote(
            Note(
                id = 77L,
                title = "Private title",
                content = "Private body",
                isInVault = true
            )
        )
        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository),
            observeVaultState = ObserveVaultStateUseCase(vaultStateRepository)
        )
        runCurrent()

        vm.onTitleChange("Unsaved private title")
        vm.onContentChange("Unsaved private body")
        vaultStateRepository.setState(VaultState.LOCKED)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isVaultNote)
        assertTrue(state.isVaultLocked)
        assertTrue(state.isReadOnly)
        assertFalse(state.isDirty)
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertTrue(state.imagePaths.isEmpty())
        assertTrue(vaultRepository.savedNotes.isEmpty())
    }

    @Test
    fun `onTitleChange updates title and marks isDirty`() = runTest {
        val vm = viewModel()
        vm.onTitleChange("New title")
        assertEquals("New title", vm.uiState.value.title)
        assertTrue(vm.uiState.value.isDirty)
    }

    @Test
    fun `onContentChange updates content and marks isDirty`() = runTest {
        val vm = viewModel()
        vm.onContentChange("Note text")
        assertEquals("Note text", vm.uiState.value.content)
        assertTrue(vm.uiState.value.isDirty)
    }

    @Test
    fun `undo and redo restore text changes in order`() = runTest {
        val vm = viewModel()

        vm.onContentChange("First")
        advanceUndoHistoryDebounce()
        vm.onContentChange("Second")
        advanceUndoHistoryDebounce()

        vm.undoContentChange()
        assertEquals("First", vm.uiState.value.content)

        vm.undoContentChange()
        assertEquals("", vm.uiState.value.content)

        vm.redoContentChange()
        assertEquals("First", vm.uiState.value.content)

        vm.redoContentChange()
        assertEquals("Second", vm.uiState.value.content)
    }

    @Test
    fun `undo commits pending debounced text before restoring`() = runTest {
        val vm = viewModel()

        vm.onContentChange("a")
        vm.onContentChange("ab")
        vm.onContentChange("abc")

        assertTrue(vm.undoRedoState.value.canUndo)
        vm.undoContentChange()

        assertEquals("", vm.uiState.value.content)
        assertTrue(vm.undoRedoState.value.canRedo)
    }

    @Test
    fun `new edit after undo clears redo stack`() = runTest {
        val vm = viewModel()

        vm.onContentChange("First")
        advanceUndoHistoryDebounce()
        vm.onContentChange("Second")
        advanceUndoHistoryDebounce()
        vm.undoContentChange()

        assertTrue(vm.undoRedoState.value.canRedo)
        vm.onContentChange("Replacement")

        assertFalse(vm.undoRedoState.value.canRedo)
    }

    @Test
    fun `clearContentHistory empties in-memory undo state`() = runTest {
        val vm = viewModel()

        vm.onContentChange("Draft")
        assertTrue(vm.undoRedoState.value.canUndo)

        vm.clearContentHistory()
        vm.undoContentChange()

        assertEquals("Draft", vm.uiState.value.content)
        assertFalse(vm.undoRedoState.value.canUndo)
        assertFalse(vm.undoRedoState.value.canRedo)
    }

    @Test
    fun `flushPendingChanges saves note when dirty and has content`() = runTest {
        val vm = viewModel()
        vm.onContentChange("Something important")
        vm.flushPendingChanges()
        advanceUntilIdle()
        assertEquals(1, fakeNoteDao.insertedCount)
        assertNotEquals(EditorViewModel.NO_ID, vm.uiState.value.noteId)
    }

    @Test
    fun `flushPendingChanges does not save empty note`() = runTest {
        val vm = viewModel()
        vm.flushPendingChanges()
        advanceUntilIdle()
        assertEquals(0, fakeNoteDao.insertedCount)
    }

    @Test
    fun `flushPendingChanges saves note created from template without user edits`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 3L, name = "T", content = "Template content")
        )
        val vm = viewModel(templateId = 3L)
        runCurrent()
        vm.flushPendingChanges()
        advanceUntilIdle()
        assertEquals(
            "Flush must save a template-created note even without user edits",
            1,
            fakeNoteDao.insertedCount
        )
    }

    @Test
    fun `togglePreview opens preview without triggering a save`() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.showPreview)
        vm.togglePreview()
        assertTrue(vm.uiState.value.showPreview)
        assertFalse(vm.uiState.value.openedDirectlyInPreview)
        assertEquals(0, fakeNoteDao.insertedCount)
    }

    @Test
    fun `togglePreview closes preview on second tap`() = runTest {
        val vm = viewModel()
        vm.togglePreview()
        assertTrue(vm.uiState.value.showPreview)
        vm.togglePreview()
        assertFalse(vm.uiState.value.showPreview)
    }

    @Test
    fun `togglePreview works on empty note`() = runTest {
        val vm = viewModel()
        assertEquals("", vm.uiState.value.content)
        vm.togglePreview()
        assertTrue("preview must open even on an empty note", vm.uiState.value.showPreview)
    }

    @Test
    fun `toggleTheme switches dark theme to LIGHT`() = runTest {
        val preferencesRepository = FakeEditorPreferencesRepository()
        val vm = viewModel(preferencesRepository = preferencesRepository)

        vm.toggleTheme(isDarkTheme = true)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, preferencesRepository.lastThemeMode)
    }

    @Test
    fun `toggleTheme switches light theme to DARK`() = runTest {
        val preferencesRepository = FakeEditorPreferencesRepository()
        val vm = viewModel(preferencesRepository = preferencesRepository)

        vm.toggleTheme(isDarkTheme = false)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferencesRepository.lastThemeMode)
    }

    private fun kotlinx.coroutines.test.TestScope.advanceUndoHistoryDebounce() {
        advanceTimeBy(DEFAULT_UNDO_HISTORY_DEBOUNCE_MS)
        runCurrent()
    }
}
