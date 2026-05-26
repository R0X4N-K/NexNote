package io.github.r0x4nk.nexnote.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionUiStateTest {

    @Test
    fun `toggle enters selection mode and toggles ids`() {
        val selected = SelectionUiState()
            .toggle(1L)
            .toggle(2L)

        assertTrue(selected.isActive)
        assertEquals(setOf(1L, 2L), selected.selectedIds)

        val deselected = selected.toggle(1L)

        assertTrue(deselected.isActive)
        assertEquals(setOf(2L), deselected.selectedIds)
    }

    @Test
    fun `select all filters invalid ids and deselect all keeps selection mode active`() {
        val state = SelectionUiState()
            .selectAll(listOf(0L, 3L, 4L))
            .deselectAll()

        assertTrue(state.isActive)
        assertEquals(emptySet<Long>(), state.selectedIds)
    }

    @Test
    fun `retain selectable ids drops removed items without leaving mode`() {
        val state = SelectionUiState(isActive = true, selectedIds = setOf(1L, 2L, 3L))
            .retainSelectableIds(listOf(2L, 3L, 5L))

        assertTrue(state.isActive)
        assertEquals(setOf(2L, 3L), state.selectedIds)
    }

    @Test
    fun `exit clears ids and selection mode`() {
        val state = SelectionUiState(isActive = true, selectedIds = setOf(1L)).exit()

        assertFalse(state.isActive)
        assertEquals(emptySet<Long>(), state.selectedIds)
    }
}
