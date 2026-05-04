package com.example.nexnote.ui.screen.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextFieldSyncPolicyTest {

    @Test
    fun `canApplyRecomposedValue skips stale empty text over loaded state`() {
        val currentValue = TextFieldValue("Loaded note", TextRange(0))
        val staleValue = TextFieldValue("")

        assertFalse(
            EditorTextFieldSyncPolicy.canApplyRecomposedValue(currentValue, staleValue)
        )
    }

    @Test
    fun `canApplyRecomposedValue skips stale loaded text over cleared state`() {
        val currentValue = TextFieldValue("")
        val staleValue = TextFieldValue("Loaded note", TextRange(11))

        assertFalse(
            EditorTextFieldSyncPolicy.canApplyRecomposedValue(currentValue, staleValue)
        )
    }

    @Test
    fun `canApplyRecomposedValue accepts selection sync for matching text`() {
        val currentValue = TextFieldValue("Loaded note", TextRange(0))
        val recomposedValue = TextFieldValue("Loaded note", TextRange(11))

        assertTrue(
            EditorTextFieldSyncPolicy.canApplyRecomposedValue(currentValue, recomposedValue)
        )
    }
}
