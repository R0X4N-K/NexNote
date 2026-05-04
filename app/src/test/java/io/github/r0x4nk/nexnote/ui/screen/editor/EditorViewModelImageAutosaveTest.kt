package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelImageAutosaveTest : EditorViewModelTestBase() {

    @Test
    fun `onImagePicked saves new note before inserting image`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val vm = viewModel(imageStorage = imageStorage)
        vm.onContentChange("Hello")

        vm.onImagePicked(
            openImageInputStream = { ByteArrayInputStream(byteArrayOf(1, 2, 3)) },
            insertionOffset = 5
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotEquals(EditorViewModel.NO_ID, state.noteId)
        assertEquals(listOf(1L), imageStorage.copiedNoteIds)
        assertEquals(listOf("images/note_1_img_100.jpg"), state.imagePaths)
        assertTrue(state.content.contains("![image](images/note_1_img_100.jpg)"))
    }

    @Test
    fun `onImagePicked does nothing in template mode`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val vm = viewModel(
            editTemplateId = EditorViewModel.NEW_TEMPLATE_ID,
            imageStorage = imageStorage
        )
        runCurrent()

        vm.onImagePicked(openImageInputStream = { ByteArrayInputStream(byteArrayOf(1)) })
        advanceUntilIdle()

        assertTrue(imageStorage.copiedNoteIds.isEmpty())
        assertEquals("", vm.uiState.value.content)
    }

    @Test
    fun `onImagePicked on empty new note asks for text first`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val vm = viewModel(imageStorage = imageStorage)

        vm.onImagePicked(openImageInputStream = { ByteArrayInputStream(byteArrayOf(1)) })
        advanceUntilIdle()

        assertTrue(imageStorage.copiedNoteIds.isEmpty())
        assertEquals("Add some text before inserting an image", vm.uiState.value.errorMessage)
    }

    @Test
    fun `onImagePicked shows insert error when storage copy fails`() = runTest {
        fakeNoteDao.addNote(NoteEntity(id = 42L, title = "Title", content = "Content"))
        val imageStorage = FakeEditorNoteImageStorage().apply { failOnCopy = true }
        val vm = viewModel(noteId = 42L, imageStorage = imageStorage)
        runCurrent()

        vm.onImagePicked(openImageInputStream = { ByteArrayInputStream(byteArrayOf(1)) })
        advanceUntilIdle()

        assertEquals("Could not insert image", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.imagePaths.isEmpty())
    }

    @Test
    fun `autosave does not fire before the 1500ms debounce`() = runTest {
        val vm = viewModel()
        vm.onContentChange("Text")
        advanceTimeBy(500L)
        assertEquals(0, fakeNoteDao.insertedCount)
    }

    @Test
    fun `autosave fires after the 1500ms debounce`() = runTest {
        val vm = viewModel()
        vm.onContentChange("Text")
        advanceTimeBy(2000L)
        assertEquals(1, fakeNoteDao.insertedCount)
    }

    @Test
    fun `clearError resets errorMessage`() = runTest {
        fakeNoteDao.failOnInsert = true
        val vm = viewModel()
        vm.onContentChange("Test")
        advanceTimeBy(2000L)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.errorMessage?.isNotBlank() == true)
        vm.clearError()
        assertNull(vm.uiState.value.errorMessage)
    }
}
