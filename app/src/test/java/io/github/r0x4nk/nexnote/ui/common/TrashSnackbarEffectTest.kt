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
        var confirmCount = 0

        handleTrashSnackbarEvent(
            event = TrashedNoteEvent(noteId = 42L, noteLabel = "Recover me"),
            showSnackbar = { SnackbarResult.ActionPerformed },
            onUndoTrash = { undoIds += it },
            onConfirmTrash = { confirmCount++ }
        )

        assertEquals(listOf(42L), undoIds)
        assertEquals(0, confirmCount)
    }

    @Test
    fun `dismissed snackbar confirms the trashed note without note id`() = runTest {
        val undoIds = mutableListOf<Long>()
        var confirmCount = 0

        handleTrashSnackbarEvent(
            event = TrashedNoteEvent(noteId = 7L, noteLabel = "Keep deleted"),
            showSnackbar = { SnackbarResult.Dismissed },
            onUndoTrash = { undoIds += it },
            onConfirmTrash = { confirmCount++ }
        )

        assertTrue(undoIds.isEmpty())
        assertEquals(1, confirmCount)
    }
}
