package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchStateTest {

    @Test
    fun `updateQuery finds all case-insensitive matches`() {
        val state = NoteSearchState.Empty
            .open("Alpha beta alpha")
            .updateQuery("ALPHA", "Alpha beta alpha")

        assertTrue(state.isActive)
        assertEquals(listOf(0..<5, 11..<16), state.matches)
        assertEquals(0..<5, state.currentMatch)
        assertEquals("1/2", state.resultLabel)
    }

    @Test
    fun `next and previous wrap around matches`() {
        val state = NoteSearchState.Empty
            .open("one two one")
            .updateQuery("one", "one two one")

        val second = state.next()
        val wrappedForward = second.next()
        val wrappedBackward = state.previous()

        assertEquals(8..<11, second.currentMatch)
        assertEquals(0..<3, wrappedForward.currentMatch)
        assertEquals(8..<11, wrappedBackward.currentMatch)
    }

    @Test
    fun `blank query clears matches while keeping search open`() {
        val state = NoteSearchState.Empty
            .open("needle")
            .updateQuery("needle", "needle")
            .updateQuery("   ", "needle")

        assertTrue(state.isActive)
        assertFalse(state.hasQuery)
        assertTrue(state.matches.isEmpty())
        assertNull(state.currentMatch)
        assertEquals("", state.resultLabel)
    }

    @Test
    fun `refresh keeps the selected match ordinal when text changes`() {
        val state = NoteSearchState.Empty
            .open("one two one")
            .updateQuery("one", "one two one")
            .next()

        val refreshed = state.refresh("zero one two one")

        assertEquals(13..<16, refreshed.currentMatch)
        assertEquals("2/2", refreshed.resultLabel)
    }

    @Test
    fun `close resets search state`() {
        val closed = NoteSearchState.Empty
            .open("needle")
            .updateQuery("needle", "needle")
            .close()

        assertEquals(NoteSearchState.Empty, closed)
    }
}
