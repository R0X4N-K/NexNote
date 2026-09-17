package io.github.r0x4nk.nexnote.ui.screen.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplatePreviewTextTest {

    @Test
    fun `resolves the date placeholder`() {
        val source = templatePreviewSource("# {{date}}\nBody", dateLabel = "14/09/2026")

        assertEquals("# 14/09/2026\nBody", source)
    }

    @Test
    fun `drops empty list markers while keeping list content`() {
        val content = """
            # Checklist
            - [ ]
            - [x]
            - [ ] Real task
            1.
            2. Real step
        """.trimIndent()

        val source = templatePreviewSource(content, dateLabel = "14/09/2026")

        assertEquals(
            """
            # Checklist
            - [ ] Real task
            2. Real step
            """.trimIndent(),
            source
        )
    }

    @Test
    fun `collapses blank line runs and trims edges`() {
        val content = "\n\n# Journal\n\n\n## Section\n\n\n"

        val source = templatePreviewSource(content, dateLabel = "14/09/2026")

        assertEquals("# Journal\n## Section", source)
    }
}
