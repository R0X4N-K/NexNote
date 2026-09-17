package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.data.db.PendingImageDeletionDao
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Only retries explicitly deleted files; never guesses references from encrypted Vault rows. */
internal class PendingImageCleanup(
    private val dao: PendingImageDeletionDao,
    private val imageStorage: NoteImageStorage
) {
    private val mutex = Mutex()

    suspend fun runOnce() = mutex.withLock {
        var after = ""
        while (true) {
            val paths = dao.nextBatch(after, BATCH_SIZE)
            if (paths.isEmpty()) break
            paths.forEach { path ->
                if (runCatchingPreservingCancellation { imageStorage.deleteImage(path) }
                        .getOrDefault(false)) {
                    dao.remove(path)
                }
            }
            // Failed entries remain queued, without starving the rest of this pass.
            after = paths.last()
        }
    }

    private companion object {
        const val BATCH_SIZE = 100
    }
}
