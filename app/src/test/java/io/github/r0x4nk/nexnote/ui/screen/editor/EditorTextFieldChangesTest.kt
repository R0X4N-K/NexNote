package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorTextFieldChangesTest {

    @Test
    fun `userTextFieldChanges skips the initial construction snapshot`() = runTest {
        val changes = flowOf(
            TextFieldValue(""),
            TextFieldValue("Loaded note"),
            TextFieldValue("Loaded note edited")
        ).userTextFieldChanges(expectedText = { "Loaded note" }).toList()

        assertEquals(
            listOf(
                TextFieldValue("Loaded note"),
                TextFieldValue("Loaded note edited")
            ),
            changes
        )
    }

    @Test
    fun `userTextFieldChanges still accepts an intentional later clear`() = runTest {
        val changes = flowOf(
            TextFieldValue(""),
            TextFieldValue("Loaded note"),
            TextFieldValue(""),
            TextFieldValue("")
        ).userTextFieldChanges(expectedText = { "Loaded note" }).toList()

        assertEquals(
            listOf(
                TextFieldValue("Loaded note"),
                TextFieldValue("")
            ),
            changes
        )
    }

    @Test
    fun `userTextFieldChanges keeps first user edit when no note content is loaded`() = runTest {
        val changes = flowOf(
            TextFieldValue("Draft")
        ).userTextFieldChanges(expectedText = { "" }).toList()

        assertEquals(listOf(TextFieldValue("Draft")), changes)
    }
}
