package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import kotlinx.coroutines.CancellationException
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
    fun `onImagePicked on empty new note creates the note and inserts the image`() = runTest {
        // Regression guard: previously a brand-new note with no title or body
        // could not accept an image — the editor refused to persist an "empty"
        // note, so it never obtained a real id and the user got a misleading
        // "Add some text before inserting an image" error. The new contract is
        // that picking an image IS a content-bearing gesture: the note is
        // eagerly persisted via [EditorSaveDelegate.ensurePersisted] and the
        // image is attached normally.
        val imageStorage = FakeEditorNoteImageStorage()
        val vm = viewModel(imageStorage = imageStorage)

        vm.onImagePicked(openImageInputStream = { ByteArrayInputStream(byteArrayOf(1)) })
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotEquals(EditorViewModel.NO_ID, state.noteId)
        assertEquals(listOf(1L), imageStorage.copiedNoteIds)
        assertEquals(listOf("images/note_1_img_100.jpg"), state.imagePaths)
        assertTrue(state.content.contains("![image](images/note_1_img_100.jpg)"))
        assertNull(state.errorMessage)
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
    fun `onImagePicked in existing vault note saves immediately through vault repository`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
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
            imageStorage = imageStorage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onImagePicked(
            openImageInputStream = { ByteArrayInputStream(byteArrayOf(1, 2, 3)) },
            insertionOffset = 0
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        val expectedPath = "images/note_77_img_100.jpg"
        assertEquals(listOf(77L), imageStorage.copiedNoteIds)
        assertEquals(listOf(expectedPath), state.imagePaths)
        assertTrue(state.content.contains("![image]($expectedPath)"))
        assertTrue(vaultRepository.savedNotes.single().isInVault)
        assertEquals(listOf(expectedPath), vaultRepository.savedNotes.single().imagePaths)
        assertEquals(0, fakeNoteDao.insertedCount)
        assertEquals(0, fakeNoteDao.updatedCount)
        assertFalse(state.isDirty)
    }

    @Test
    fun `onImagePicked in vault note rolls back copied image when immediate save fails`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val vaultRepository = FakeEditorVaultNoteRepository().apply {
            failOnSave = true
            addNote(
                Note(
                    id = 77L,
                    title = "Private title",
                    content = "Private body",
                    isInVault = true
                )
            )
        }
        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            imageStorage = imageStorage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onImagePicked(
            openImageInputStream = { ByteArrayInputStream(byteArrayOf(1, 2, 3)) },
            insertionOffset = 0
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        val copiedPath = "images/note_77_img_100.jpg"
        assertEquals(listOf(77L), imageStorage.copiedNoteIds)
        assertEquals(listOf(copiedPath), imageStorage.deletedPaths)
        assertEquals("Private body", state.content)
        assertTrue(state.imagePaths.isEmpty())
        assertFalse(state.content.contains(copiedPath))
        assertEquals("Could not insert image", state.errorMessage)
        assertTrue(vaultRepository.savedNotes.isEmpty())
        assertFalse(state.isDirty)
        assertEquals(0, fakeNoteDao.insertedCount)
        assertEquals(0, fakeNoteDao.updatedCount)
    }

    @Test
    fun `onImagePicked cancellation rolls back copied image without ordinary error`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage().apply {
            deleteFailure = IllegalStateException("cleanup failed")
        }
        val vaultRepository = FakeEditorVaultNoteRepository().apply {
            saveFailure = CancellationException("save cancelled")
            addNote(
                Note(
                    id = 77L,
                    title = "Private title",
                    content = "Private body",
                    isInVault = true
                )
            )
        }
        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            imageStorage = imageStorage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onImagePicked(openImageInputStream = { ByteArrayInputStream(byteArrayOf(1)) })
        advanceUntilIdle()

        val copiedPath = "images/note_77_img_100.jpg"
        val state = vm.uiState.value
        assertEquals(listOf(copiedPath), imageStorage.deletedPaths)
        assertEquals("Private body", state.content)
        assertTrue(state.imagePaths.isEmpty())
        assertFalse(state.isDirty)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onImagePicked in new vault note creates vault note before copying image`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val vaultRepository = FakeEditorVaultNoteRepository()
        val vm = viewModel(
            mode = EditorMode.NewVaultNote,
            imageStorage = imageStorage,
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onImagePicked(openImageInputStream = { ByteArrayInputStream(byteArrayOf(1)) })
        advanceUntilIdle()

        val state = vm.uiState.value
        val expectedPath = "images/note_${state.noteId}_img_100.jpg"
        assertNotEquals(EditorViewModel.NO_ID, state.noteId)
        assertEquals(listOf(state.noteId), imageStorage.copiedNoteIds)
        assertEquals(listOf(expectedPath), state.imagePaths)
        assertTrue(state.content.contains("![image]($expectedPath)"))
        assertEquals(2, vaultRepository.savedNotes.size)
        assertTrue(vaultRepository.savedNotes.first().imagePaths.isEmpty())
        assertEquals(listOf(expectedPath), vaultRepository.savedNotes.last().imagePaths)
        assertTrue(vaultRepository.savedNotes.all { it.isInVault })
        assertEquals(0, fakeNoteDao.insertedCount)
        assertFalse(state.isDirty)
    }

    @Test
    fun `onRemoveImage in vault note saves updated image list immediately`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val vaultRepository = FakeEditorVaultNoteRepository()
        val imagePath = "images/note_77_img_100.jpg"
        vaultRepository.addNote(
            Note(
                id = 77L,
                title = "Private title",
                content = "Private body\n\n![image]($imagePath)",
                imagePaths = listOf(imagePath),
                isInVault = true
            )
        )
        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            imageStorage = imageStorage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onRemoveImage(imagePath)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(imagePath), imageStorage.deletedPaths)
        assertTrue(state.imagePaths.isEmpty())
        assertFalse(state.content.contains(imagePath))
        assertTrue(vaultRepository.savedNotes.single().imagePaths.isEmpty())
        assertTrue(vaultRepository.savedNotes.single().isInVault)
        assertFalse(state.isDirty)
    }

    @Test
    fun `onRemoveImage in vault note restores image when immediate save fails`() = runTest {
        val imageStorage = FakeEditorNoteImageStorage()
        val imagePath = "images/note_77_img_100.jpg"
        val vaultRepository = FakeEditorVaultNoteRepository().apply {
            failOnSave = true
            addNote(
                Note(
                    id = 77L,
                    title = "Private title",
                    content = "Private body\n\n![image]($imagePath)",
                    imagePaths = listOf(imagePath),
                    isInVault = true
                )
            )
        }
        val vm = viewModel(
            mode = EditorMode.VaultNote(77L),
            imageStorage = imageStorage,
            getVaultNoteById = GetVaultNoteByIdUseCase(vaultRepository),
            saveVaultNote = SaveVaultNoteUseCase(vaultRepository)
        )
        runCurrent()

        vm.onRemoveImage(imagePath)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(imageStorage.deletedPaths.isEmpty())
        assertEquals(listOf(imagePath), state.imagePaths)
        assertTrue(state.content.contains(imagePath))
        assertEquals("Could not remove image", state.errorMessage)
        assertTrue(vaultRepository.savedNotes.isEmpty())
        assertFalse(state.isDirty)
        assertEquals(0, fakeNoteDao.insertedCount)
        assertEquals(0, fakeNoteDao.updatedCount)
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
