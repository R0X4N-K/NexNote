package com.example.nexnote.util

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
}
