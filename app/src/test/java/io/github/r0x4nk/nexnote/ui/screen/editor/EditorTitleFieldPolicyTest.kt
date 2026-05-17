package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTitleFieldPolicyTest {

    @Test
    fun `editable focused title expands`() {
        assertTrue(EditorTitleFieldPolicy.isExpanded(hasFocus = true, readOnly = false))
    }

    @Test
    fun `readonly title stays collapsed even when focused`() {
        assertFalse(EditorTitleFieldPolicy.isExpanded(hasFocus = true, readOnly = true))
    }

    @Test
    fun `unfocused title stays collapsed`() {
        assertFalse(EditorTitleFieldPolicy.isExpanded(hasFocus = false, readOnly = false))
    }

    @Test
    fun `expanded title allows three visual lines`() {
        assertEquals(3, EditorTitleFieldPolicy.maxLines(isExpanded = true))
    }

    @Test
    fun `collapsed title uses one visual line`() {
        assertEquals(1, EditorTitleFieldPolicy.maxLines(isExpanded = false))
    }

    @Test
    fun `normalizeInput keeps the stored title single line`() {
        assertEquals(
            "First second third fourth",
            EditorTitleFieldPolicy.normalizeInput("First\nsecond\rthird\r\nfourth")
        )
    }
}
