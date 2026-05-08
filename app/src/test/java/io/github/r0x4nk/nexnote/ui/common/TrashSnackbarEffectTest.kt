package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrashSnackbarEffectTest {

    @Test
    fun `action performed dispatches undo for the trashed note id`() = runTest {
        val undoIds = mutableListOf<Long>()
        val confirmIds = mutableListOf<Long>()

        handleTrashSnackbarEvent(
            event = TrashedNoteEvent(noteId = 42L, noteLabel = "Recover me"),
            showSnackbar = { SnackbarResult.ActionPerformed },
            onUndoTrash = { undoIds += it },
            onConfirmTrash = { confirmIds += it }
        )

        assertEquals(listOf(42L), undoIds)
        assertTrue(confirmIds.isEmpty())
    }

    @Test
    fun `dismissed snackbar confirms the trashed note id`() = runTest {
        val undoIds = mutableListOf<Long>()
        val confirmIds = mutableListOf<Long>()

        handleTrashSnackbarEvent(
            event = TrashedNoteEvent(noteId = 7L, noteLabel = "Keep deleted"),
            showSnackbar = { SnackbarResult.Dismissed },
            onUndoTrash = { undoIds += it },
            onConfirmTrash = { confirmIds += it }
        )

        assertTrue(undoIds.isEmpty())
        assertEquals(listOf(7L), confirmIds)
    }
}
