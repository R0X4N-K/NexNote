package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorVaultPickerIntegrationTest : EditorViewModelTestBase() {
    @Test fun `picker saves edits before leaving and resumes the same note after unlock`() = runTest {
        val vault = FakeEditorVaultNoteRepository().apply {
            addNote(Note(id = 42, content = "Original", isInVault = true))
        }
        val session = FakeEditorVaultStateRepository()
        val vm = viewModel(mode = EditorMode.VaultNote(42),
            getVaultNoteById = GetVaultNoteByIdUseCase(vault),
            saveVaultNote = SaveVaultNoteUseCase(vault),
            observeVaultState = ObserveVaultStateUseCase(session))
        runCurrent()
        vm.onContentChange("Edited before picker")
        assertTrue(vm.pendingAttachment.prepare())
        assertEquals("Edited before picker", vault.getVaultNoteById(42)!!.content)
        session.lock()
        runCurrent()
        var calls = 0
        vm.pendingAttachment.accept { calls++ }
        vm.pendingAttachment.resume()
        assertEquals(0, calls)
        assertEquals("", vm.uiState.value.content)
        session.setState(VaultState.UNLOCKED)
        runCurrent()
        vm.pendingAttachment.resume()
        assertEquals(1, calls)
        assertEquals("Edited before picker", vm.uiState.value.content)
        assertFalse(vm.uiState.value.isVaultLocked)
    }

    @Test fun `image copy completing after lock cannot restore protected editor content`() = runTest {
        exerciseImageCompletionAfterLock(false)
    }

    @Test fun `cancelled image copy after lock cannot restore protected editor content`() = runTest {
        exerciseImageCompletionAfterLock(true)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.exerciseImageCompletionAfterLock(cancel: Boolean) {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val deleted = mutableListOf<String>()
        val storage = object : NoteImageStorage by FakeEditorNoteImageStorage() {
            override suspend fun copyImageToInternal(noteId: Long, openInputStream: () -> InputStream?): String {
                started.complete(Unit)
                gate.await()
                if (cancel) throw CancellationException("Interrupted import")
                return "images/pending.jpg"
            }
            override suspend fun deleteImage(relativePath: String): Boolean {
                deleted += relativePath
                return true
            }
        }
        val vault = FakeEditorVaultNoteRepository().apply {
            addNote(Note(id = 42, content = "Secret", isInVault = true))
        }
        val session = FakeEditorVaultStateRepository()
        val vm = viewModel(mode = EditorMode.VaultNote(42), imageStorage = storage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vault), saveVaultNote = SaveVaultNoteUseCase(vault),
            observeVaultState = ObserveVaultStateUseCase(session))
        runCurrent()
        vm.onImagePicked({ null })
        started.await()
        session.lock()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.uiState.value.isVaultLocked)
        assertEquals("", vm.uiState.value.content)
        assertTrue(vm.uiState.value.imagePaths.isEmpty())
        if (!cancel) assertEquals(listOf("images/pending.jpg"), deleted)
    }
}
