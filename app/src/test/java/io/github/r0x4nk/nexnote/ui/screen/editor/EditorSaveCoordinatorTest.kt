package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorSaveCoordinatorTest {

    @Test
    fun `final and normal saves execute in FIFO order without overlap`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val owner = CoroutineScope(SupervisorJob() + dispatcher)
        val coordinator = EditorSaveCoordinator(owner, dispatcher)
        val firstMayFinish = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()

        val first = coordinator.enqueueFinalSave {
            events += "first-start"
            firstMayFinish.await()
            events += "first-end"
            true
        }
        val second = async {
            coordinator.runSave {
                events += "second"
                true
            }
        }

        runCurrent()
        assertEquals(listOf("first-start"), events)
        assertFalse(second.isCompleted)

        firstMayFinish.complete(Unit)
        runCurrent()

        assertTrue(first.await())
        assertTrue(second.await())
        assertEquals(listOf("first-start", "first-end", "second"), events)
        owner.cancel()
    }

    @Test
    fun `cancelled queued caller is skipped and does not stop following saves`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val owner = CoroutineScope(SupervisorJob() + dispatcher)
        val coordinator = EditorSaveCoordinator(owner, dispatcher)
        val releaseFirst = CompletableDeferred<Unit>()
        var cancelledBlockRan = false
        var followingBlockRan = false
        coordinator.enqueueFinalSave {
            releaseFirst.await()
            true
        }
        val cancelled = async {
            coordinator.runSave {
                cancelledBlockRan = true
                true
            }
        }
        val following = async {
            coordinator.runSave {
                followingBlockRan = true
                true
            }
        }

        runCurrent()
        cancelled.cancel()
        releaseFirst.complete(Unit)
        runCurrent()

        assertTrue(cancelled.isCancelled)
        assertFalse(cancelledBlockRan)
        assertTrue(following.await())
        assertTrue(followingBlockRan)
        owner.cancel()
    }

    @Test
    fun `owner shutdown cancels active and queued final saves`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val owner = CoroutineScope(SupervisorJob() + dispatcher)
        val coordinator = EditorSaveCoordinator(owner, dispatcher)
        val activeStarted = CompletableDeferred<Unit>()
        val neverReleased = CompletableDeferred<Unit>()
        var queuedBlockRan = false
        val active = coordinator.enqueueFinalSave {
            activeStarted.complete(Unit)
            neverReleased.await()
            true
        }
        val queued = coordinator.enqueueFinalSave {
            queuedBlockRan = true
            true
        }

        runCurrent()
        activeStarted.await()
        owner.cancel()
        runCurrent()

        assertTrue(active.isCancelled)
        assertTrue(queued.isCancelled)
        assertFalse(queuedBlockRan)
    }
}
