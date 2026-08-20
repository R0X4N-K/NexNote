package io.github.r0x4nk.nexnote.util

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineResultTest {

    @Test
    fun `ordinary failure is represented as Result failure`() {
        val result = runCatchingPreservingCancellation<Int> {
            throw IllegalStateException("ordinary")
        }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `cancellation is rethrown instead of represented as Result failure`() {
        var thrown: Throwable? = null
        try {
            runCatchingPreservingCancellation<Int> {
                throw CancellationException("cancel")
            }
        } catch (error: Throwable) {
            thrown = error
        }

        assertTrue(thrown is CancellationException)
        assertEquals("cancel", thrown?.message)
    }
}
