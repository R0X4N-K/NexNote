package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.data.db.PendingImageDeletionDao
import io.github.r0x4nk.nexnote.data.db.entity.PendingImageDeletionEntity
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PendingImageCleanupTest {
    @Test fun failedFilesRemainQueuedAndDoNotStarveLaterBatches() = runTest {
        val dao = QueueDao((0..204).map { "images/%03d.jpg".format(it) })
        var failDeletion = true
        val storage = object : NoteImageStorage by NoOpNoteImageStorage() {
            override suspend fun deleteImage(relativePath: String): Boolean =
                !(failDeletion && relativePath == "images/000.jpg")
        }
        PendingImageCleanup(dao, storage).runOnce()
        assertEquals(listOf("images/000.jpg"), dao.paths.toList())
        failDeletion = false
        PendingImageCleanup(dao, storage).runOnce()
        assertTrue(dao.paths.isEmpty())
    }

    @Test fun cancellationPreservesPendingEntry() = runTest {
        val dao = QueueDao(listOf("images/a.jpg"))
        val storage = object : NoteImageStorage by NoOpNoteImageStorage() {
            override suspend fun deleteImage(relativePath: String): Boolean =
                throw CancellationException("Interrupted")
        }
        try {
            PendingImageCleanup(dao, storage).runOnce()
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(listOf("images/a.jpg"), dao.paths.toList())
    }

    private class QueueDao(initial: List<String>) : PendingImageDeletionDao {
        val paths = initial.toSortedSet()
        override suspend fun enqueue(entries: List<PendingImageDeletionEntity>) {
            paths.addAll(entries.map { it.relativePath })
        }
        override suspend fun nextBatch(after: String, limit: Int): List<String> =
            paths.filter { it > after }.take(limit)
        override suspend fun remove(path: String) { paths.remove(path) }
    }
}
