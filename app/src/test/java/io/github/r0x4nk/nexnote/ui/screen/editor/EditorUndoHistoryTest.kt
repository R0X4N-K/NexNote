package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorUndoHistoryTest {

    @Test
    fun `debounce groups rapid typing into one undo step`() = runTest {
        val history = history()

        history.recordUserChange(snapshot("a"))
        history.recordUserChange(snapshot("ab"))
        history.recordUserChange(snapshot("abc"))
        advanceHistoryDebounce()

        assertEquals("", history.undo()?.text)
        assertEquals("abc", history.redo()?.text)
    }

    @Test
    fun `undo and redo restore snapshots in order`() = runTest {
        val history = history()

        history.recordUserChange(snapshot("first"))
        advanceHistoryDebounce()
        history.recordUserChange(snapshot("second"))
        advanceHistoryDebounce()

        assertEquals("first", history.undo()?.text)
        assertEquals("", history.undo()?.text)
        assertEquals("first", history.redo()?.text)
        assertEquals("second", history.redo()?.text)
    }

    @Test
    fun `stack keeps only the configured number of snapshots`() = runTest {
        val history = history(maxStackSize = 3)

        repeat(5) { index ->
            history.recordUserChange(snapshot("state-${index + 1}"))
            advanceHistoryDebounce()
        }

        assertEquals("state-4", history.undo()?.text)
        assertEquals("state-3", history.undo()?.text)
        assertEquals("state-2", history.undo()?.text)
        assertNull(history.undo())
    }

    @Test
    fun `immediate changes commit pending typing before their own snapshot`() = runTest {
        val history = history()

        history.recordUserChange(snapshot("typed"))
        history.recordImmediateChange(
            previous = snapshot("typed"),
            next = snapshot("typed\n![image](image.jpg)")
        )

        assertEquals("typed", history.undo()?.text)
        assertEquals("", history.undo()?.text)
    }

    @Test
    fun `clear releases undo and redo state`() = runTest {
        val history = history()

        history.recordUserChange(snapshot("draft"))
        advanceHistoryDebounce()
        assertTrue(history.state.value.canUndo)

        history.clear()

        assertFalse(history.state.value.canUndo)
        assertFalse(history.state.value.canRedo)
        assertNull(history.undo())
        assertNull(history.redo())
    }

    private fun TestScope.history(
        maxStackSize: Int = 50
    ): EditorUndoHistory {
        return EditorUndoHistory(
            scope = this,
            debounceMs = TEST_DEBOUNCE_MS,
            maxStackSize = maxStackSize
        )
    }

    private fun snapshot(text: String): EditorContentSnapshot {
        return EditorContentSnapshot(text = text, selectionOffset = text.length)
    }

    private fun TestScope.advanceHistoryDebounce() {
        advanceTimeBy(TEST_DEBOUNCE_MS)
        runCurrent()
    }

    private companion object {
        const val TEST_DEBOUNCE_MS = 400L
    }
}
