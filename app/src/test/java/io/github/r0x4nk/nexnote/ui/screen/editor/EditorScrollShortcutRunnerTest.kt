package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorScrollShortcutRunnerTest {

    @Test
    fun `launch cancels in-flight scroll before running the next command`() = runTest {
        val events = mutableListOf<String>()
        val runner = EditorScrollShortcutRunner(this)

        runner.launch {
            events += "bottom-start"
            delay(1_000)
            events += "bottom-end"
        }
        runCurrent()

        runner.launch {
            events += "top"
        }
        runCurrent()
        advanceUntilIdle()

        assertEquals(listOf("bottom-start", "top"), events)
    }

    @Test
    fun `cancel stops the active scroll command`() = runTest {
        val events = mutableListOf<String>()
        val runner = EditorScrollShortcutRunner(this)

        runner.launch {
            delay(1_000)
            events += "finished"
        }
        runCurrent()

        runner.cancel()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
    }
}
