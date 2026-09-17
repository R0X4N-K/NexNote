package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import java.lang.reflect.Proxy
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRepositorySnapshotTest {

    @Test
    fun fullSnapshotReleasesSubscriptionAsSoonAsExportReceivesIt() = runTest {
        val source = SnapshotSource()
        val repository = NoteRepositoryImpl(source.dao, NoOpNoteImageStorage(), backgroundScope)

        assertEquals(0, source.activeSubscriptions)
        assertEquals("Original", repository.allNotes.first().single().title)
        assertEquals(0, source.activeSubscriptions)
    }

    @Test
    fun subsequentExportReadsCurrentNotesInsteadOfReplayingPreviousSnapshot() = runTest {
        val source = SnapshotSource()
        val repository = NoteRepositoryImpl(source.dao, NoOpNoteImageStorage(), backgroundScope)

        assertEquals("Original", repository.allNotes.first().single().title)
        source.notes = listOf(NoteEntity(id = 2, title = "Replacement"))

        val exported = repository.allNotes.first()
        assertEquals(listOf(2L), exported.map { it.id })
        assertEquals("Replacement", exported.single().title)
        source.notes = emptyList()
        assertTrue(repository.allNotes.first().isEmpty())
    }

    /** A Room-like cold query: each subscription reads the current stored rows. */
    private class SnapshotSource {
        var notes = listOf(NoteEntity(id = 1, title = "Original"))
        var activeSubscriptions = 0
            private set

        private val snapshots = flow {
            activeSubscriptions++
            try {
                emit(notes)
                awaitCancellation()
            } finally {
                activeSubscriptions--
            }
        }

        val dao: NoteDao = Proxy.newProxyInstance(
            NoteDao::class.java.classLoader,
            arrayOf(NoteDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAllNotes" -> snapshots
                "getAllCreationDates", "observeAllNormalNoteCount", "getAllNotesSortedAsc",
                "getDeletedNotes", "getNoteLinkCandidates" -> emptyFlow<Nothing>()
                else -> error("Unexpected DAO call: ${method.name}")
            }
        } as NoteDao
    }
}
