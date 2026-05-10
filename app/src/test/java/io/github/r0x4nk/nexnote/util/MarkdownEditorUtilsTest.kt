package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditorUtilsTest {

    @Test
    fun `insertStandaloneMarkdownBlock inserts block at cursor inside text`() {
        val result = insertStandaloneMarkdownBlock(
            text   = "Before after",
            block  = "![image](images/photo.jpg)",
            offset = "Before".length
        )

        assertEquals(
            "Before\n![image](images/photo.jpg)\n after",
            result.text
        )
        assertEquals("Before\n![image](images/photo.jpg)\n".length, result.cursorOffset)
    }

    @Test
    fun `insertStandaloneMarkdownBlock avoids duplicate leading newline`() {
        val result = insertStandaloneMarkdownBlock(
            text   = "Before\nAfter",
            block  = "![image](images/photo.jpg)",
            offset = "Before\n".length
        )

        assertEquals(
            "Before\n![image](images/photo.jpg)\nAfter",
            result.text
        )
        assertEquals("Before\n![image](images/photo.jpg)\n".length, result.cursorOffset)
    }

    @Test
    fun `insertStandaloneMarkdownBlock reuses existing trailing newline`() {
        val result = insertStandaloneMarkdownBlock(
            text   = "Before\nAfter",
            block  = "![image](images/photo.jpg)",
            offset = "Before".length
        )

        assertEquals(
            "Before\n![image](images/photo.jpg)\nAfter",
            result.text
        )
        assertEquals("Before\n![image](images/photo.jpg)\n".length, result.cursorOffset)
    }

    @Test
    fun `insertStandaloneMarkdownBlock appends newline in empty text`() {
        val result = insertStandaloneMarkdownBlock(
            text   = "",
            block  = "![image](images/photo.jpg)",
            offset = 0
        )

        assertEquals("![image](images/photo.jpg)\n", result.text)
        assertEquals(result.text.length, result.cursorOffset)
    }

    @Test
    fun `handleSmartEnter continues blockquote`() {
        val oldValue = TextFieldValue("> first")
        val newValue = TextFieldValue("> first\n", TextRange("> first\n".length))

        val result = handleSmartEnter(oldValue, newValue)

        assertEquals("> first\n> ", result.text)
        assertEquals(TextRange("> first\n> ".length), result.selection)
    }

    @Test
    fun `handleSmartEnter stops empty blockquote`() {
        val oldValue = TextFieldValue("> first\n> ")
        val newValue = TextFieldValue("> first\n> \n", TextRange("> first\n> \n".length))

        assertEquals(newValue, handleSmartEnter(oldValue, newValue))
    }
}
