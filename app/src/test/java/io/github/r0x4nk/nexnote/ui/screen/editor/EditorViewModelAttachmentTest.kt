package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteAttachmentStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import java.io.InputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelAttachmentTest : EditorViewModelTestBase() {
    @Test fun `attachment as first edit persists immediately and undo redo retains payload`() = runTest {
        val storage = FakeAttachmentStorage()
        val vm = viewModel(imageStorage = storage)
        runCurrent()
        vm.onAttachmentPicked({ "Report.pdf" }, { "pdf".byteInputStream() }, 0)
        vm.uiState.first { it.imagePaths.isNotEmpty() && !it.isDirty }
        advanceUntilIdle()
        val content = vm.uiState.value.content
        assertTrue(content.contains("[Report.pdf]"))
        assertEquals(listOf(1L), storage.owners)
        vm.undoContentChange()
        advanceUntilIdle()
        assertEquals("", vm.uiState.value.content)
        assertTrue(storage.deleted.isEmpty())
        vm.redoContentChange()
        advanceUntilIdle()
        assertEquals(content, vm.uiState.value.content)
        assertEquals(listOf(storage.path), vm.uiState.value.imagePaths)
    }

    @Test fun `vault save failure removes imported payload and preserves existing content`() = runTest {
        val storage = FakeAttachmentStorage()
        val vault = FakeEditorVaultNoteRepository().apply {
            addNote(Note(id = 42, content = "Existing", isInVault = true))
            failOnSave = true
        }
        val vm = viewModel(mode = EditorMode.VaultNote(42), imageStorage = storage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vault), saveVaultNote = SaveVaultNoteUseCase(vault))
        runCurrent()
        vm.onAttachmentPicked({ "Report.pdf" }, { "pdf".byteInputStream() }, 8)
        vm.uiState.first { it.errorMessage == "Could not save attachment" }
        advanceUntilIdle()
        assertEquals("Existing", vm.uiState.value.content.trim())
        assertTrue(vm.uiState.value.imagePaths.isEmpty())
        assertEquals(listOf(storage.path), storage.deleted)
    }

    @Test fun `typing during slow import is preserved`() = runTest {
        val storage = FakeAttachmentStorage().apply { gate = CompletableDeferred() }
        val vm = viewModel(imageStorage = storage)
        runCurrent()
        vm.onAttachmentPicked({ "Report.pdf" }, { "pdf".byteInputStream() }, 0)
        storage.started.await()
        vm.onContentChange("Typed while importing")
        storage.gate!!.complete(Unit)
        vm.uiState.first { it.imagePaths.isNotEmpty() && !it.isDirty }
        assertTrue(vm.uiState.value.content.contains("Typed while importing"))
    }

    @Test fun `template mode refuses attachments without opening provider`() = runTest {
        val storage = FakeAttachmentStorage()
        val vm = viewModel(editTemplateId = EditorViewModel.NEW_TEMPLATE_ID, imageStorage = storage)
        runCurrent()
        vm.onAttachmentPicked({ error("Must not query provider") }, { error("Must not read provider") }, 0)
        advanceUntilIdle()
        assertTrue(storage.owners.isEmpty())
    }
}

private class FakeAttachmentStorage : NoteImageStorage by FakeEditorNoteImageStorage(), NoteAttachmentStorage {
    val path = "images/attachments/note_1_abcd.pdf"
    val owners = mutableListOf<Long>()
    val deleted = mutableListOf<String>()
    val started = CompletableDeferred<Unit>()
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun copyAttachmentToInternal(noteId: Long, fileName: String, openInputStream: () -> InputStream?): String {
        owners += noteId
        started.complete(Unit)
        gate?.await()
        openInputStream()?.use { it.readBytes() }
        return path
    }
    override suspend fun deleteImage(relativePath: String): Boolean { deleted += relativePath; return true }
}
