package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.CancellationException

import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTemplateTest : EditorViewModelTestBase() {

    @Test
    fun `loadTemplate populates content from template`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 5L, name = "My template", content = "Template content", isMarkdown = true)
        )
        val vm = viewModel(templateId = 5L)
        runCurrent()
        val state = vm.uiState.value
        assertEquals("Template content", state.content)
        assertEquals(EditorViewModel.NO_ID, state.noteId)
    }

    @Test
    fun `loadTemplate marks isDirty to guarantee the note is saved`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 5L, name = "Template", content = "Pre-filled text")
        )
        val vm = viewModel(templateId = 5L)
        runCurrent()
        assertTrue(
            "Template content must mark the note as dirty to ensure it is saved even without user edits",
            vm.uiState.value.isDirty
        )
    }

    @Test
    fun `loadTemplate schedules autosave and note is saved automatically`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 5L, name = "Template", content = "Pre-filled text")
        )
        val vm = viewModel(templateId = 5L)
        runCurrent()

        assertEquals("No insert should happen before the delay", 0, fakeNoteDao.insertedCount)

        advanceTimeBy(2000L)

        assertEquals(
            "Note must be saved automatically after the debounce",
            1,
            fakeNoteDao.insertedCount
        )
    }

    @Test
    fun `loadVaultTemplate schedules autosave through vault repository only`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 5L, name = "Template", content = "Private pre-filled text")
        )
        val vaultRepository = FakeEditorVaultNoteRepository()
        val vm = viewModel(
            mode = EditorMode.NewVaultFromTemplate(5L),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        val loadedState = vm.uiState.value
        assertTrue(loadedState.isVaultNote)
        assertTrue(loadedState.isDirty)
        assertEquals("Private pre-filled text", loadedState.content)
        assertEquals(EditorViewModel.NO_ID, loadedState.noteId)
        assertEquals(0, fakeNoteDao.insertedCount)

        advanceTimeBy(2000L)

        assertEquals(
            "Vault template-created note must not touch the normal note repository",
            0,
            fakeNoteDao.insertedCount
        )
        assertEquals(1, vaultRepository.savedNotes.size)
        assertEquals("Private pre-filled text", vaultRepository.savedNotes.single().content)
        assertTrue(vaultRepository.savedNotes.single().isInVault)
        assertNotEquals(EditorViewModel.NO_ID, vm.uiState.value.noteId)
    }

    @Test
    fun `loadTemplate replaces date placeholder`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 7L, name = "Journal", content = "Today is {{date}}, I wrote…")
        )
        val vm = viewModel(templateId = 7L)
        runCurrent()
        assertFalse(
            "The {{date}} placeholder must be replaced with a real date",
            vm.uiState.value.content.contains("{{date}}")
        )
    }

    @Test
    fun `loadTemplate with unknown id leaves state empty without error`() = runTest {
        val vm = viewModel(templateId = 999L)
        runCurrent()
        assertEquals("", vm.uiState.value.content)
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isDirty)
    }

    @Test
    fun `loadTemplateForEdit with NEW_TEMPLATE_ID opens empty template editor`() = runTest {
        val vm = viewModel(editTemplateId = EditorViewModel.NEW_TEMPLATE_ID)
        runCurrent()
        val state = vm.uiState.value
        assertTrue(state.isTemplateMode)
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertEquals(EditorViewModel.NO_ID, state.templateId)
    }

    @Test
    fun `loadTemplateForEdit with existing id loads template`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 10L, name = "My template", content = "Body", isMarkdown = true)
        )
        val vm = viewModel(editTemplateId = 10L)
        runCurrent()
        val state = vm.uiState.value
        assertTrue(state.isTemplateMode)
        assertEquals("My template", state.title)
        assertEquals("Body", state.content)
        assertEquals(10L, state.templateId)
        assertFalse(state.isDirty)
    }

    @Test
    fun `loadTemplateForEdit with unknown id sets errorMessage`() = runTest {
        val vm = viewModel(editTemplateId = 999L)
        runCurrent()
        assertTrue(vm.uiState.value.errorMessage?.isNotBlank() == true)
    }

    @Test
    fun `flushPendingChanges in template mode saves to templateRepository`() = runTest {
        val vm = viewModel(editTemplateId = EditorViewModel.NEW_TEMPLATE_ID)
        runCurrent()
        vm.onTitleChange("New template")
        vm.onContentChange("Template body")
        vm.flushPendingChanges()
        advanceUntilIdle()
        assertEquals(
            "Template mode save must not touch noteRepository",
            0,
            fakeNoteDao.insertedCount
        )
        assertNotEquals(EditorViewModel.NO_ID, vm.uiState.value.templateId)
    }

    @Test
    fun `flushPendingChanges propagates template save cancellation without ordinary error`() = runTest {
        fakeTemplateDao.insertFailure = CancellationException("cancel template save")
        val vm = viewModel(editTemplateId = EditorViewModel.NEW_TEMPLATE_ID)
        runCurrent()
        vm.onTitleChange("Pending template")

        var thrown: Throwable? = null
        try {
            vm.flushPendingChanges()
        } catch (error: Throwable) {
            thrown = error
        }

        assertTrue(thrown is CancellationException)
        assertNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.isDirty)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `flushPendingChanges in template mode updates existing template`() = runTest {
        fakeTemplateDao.addTemplate(
            TemplateEntity(id = 10L, name = "Old", content = "Old content")
        )
        val vm = viewModel(editTemplateId = 10L)
        runCurrent()
        vm.onContentChange("Updated content")
        vm.flushPendingChanges()
        advanceUntilIdle()
        assertEquals(0, fakeNoteDao.insertedCount)
        assertEquals(10L, vm.uiState.value.templateId)
    }
}
