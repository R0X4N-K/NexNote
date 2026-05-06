package io.github.r0x4nk.nexnote.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownPlainTextTest {

    @Test
    fun `fromMarkdown removes common markdown syntax`() {
        val markdown = """
            # Heading
            **Bold** and [link](https://example.com)
            - [x] Done
            `code`
        """.trimIndent()

        val result = MarkdownPlainText.fromMarkdown(markdown)

        assertEquals(
            """
            Heading
            Bold and link
            ☑ Done
            code
            """.trimIndent(),
            result
        )
    }

    @Test
    fun `fromMarkdown renders tables without pipe syntax`() {
        val markdown = """
            | Name | Count |
            | --- | ---: |
            | Pens | 3 |
        """.trimIndent()

        val result = MarkdownPlainText.fromMarkdown(markdown)

        assertEquals("Name\tCount\nPens\t3", result)
    }
}
