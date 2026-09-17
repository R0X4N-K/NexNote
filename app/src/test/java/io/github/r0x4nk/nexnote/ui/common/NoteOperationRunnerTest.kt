package io.github.r0x4nk.nexnote.ui.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteOperationRunnerTest {
    @Test fun repeatedTapsAreIgnoredWhileWorkIsPending() = runTest {
        val runner = NoteOperationRunner(this)
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        runner.launch("Deleting", { throw it }) { calls++; gate.await() }
        runner.launch("Deleting", { throw it }) { calls++ }
        runCurrent()
        assertEquals(1, calls)
        assertEquals("Deleting", runner.progress.value)
        gate.complete(Unit)
        runCurrent()
        assertNull(runner.progress.value)
    }

    @Test fun failureReleasesProgressAndAllowsRetry() = runTest {
        val runner = NoteOperationRunner(this)
        var errors = 0
        var completed = false
        runner.launch("Saving", { errors++ }) { error("disk full") }
        runCurrent()
        assertEquals(1, errors)
        assertNull(runner.progress.value)
        runner.launch("Saving", { throw it }) { completed = true }
        runCurrent()
        assertTrue(completed)
    }
}
