package io.github.r0x4nk.nexnote.fileimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFileImportParserTest {

    @Test
    fun `parse creates imported file with filename title and content`() {
        val result = TextFileImportParser.parse(
            displayName = "Meeting notes.md",
            bytes = "# Agenda\n- One".toByteArray(Charsets.UTF_8)
        )

        val parsed = result as TextFileImportParseResult.Parsed
        assertEquals("Meeting notes", parsed.file.title)
        assertEquals("# Agenda\n- One", parsed.file.content)
    }

    @Test
    fun `parse removes utf8 bom without trimming user content`() {
        val result = TextFileImportParser.parse(
            displayName = "draft.txt",
            bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                "  keep surrounding whitespace  ".toByteArray(Charsets.UTF_8)
        )

        val parsed = result as TextFileImportParseResult.Parsed
        assertEquals("  keep surrounding whitespace  ", parsed.file.content)
    }

    @Test
    fun `title falls back when display name is blank`() {
        assertEquals("Imported note", TextFileImportParser.titleFromDisplayName("   "))
    }

    @Test
    fun `title preserves hidden filenames`() {
        assertEquals(".env", TextFileImportParser.titleFromDisplayName(".env"))
    }

    @Test
    fun `title removes only final extension from path-like fallback names`() {
        assertEquals(
            "archive.notes",
            TextFileImportParser.titleFromDisplayName("primary:Download/archive.notes.md")
        )
    }

    @Test
    fun `title is capped to safe length`() {
        val title = TextFileImportParser.titleFromDisplayName("a".repeat(240) + ".md")

        assertEquals(160, title.length)
    }

    @Test
    fun `parse rejects content over max editor length`() {
        val result = TextFileImportParser.parse(
            displayName = "large.md",
            bytes = "a".repeat(TextFileImportParser.MAX_CONTENT_CHARS + 1)
                .toByteArray(Charsets.UTF_8)
        )

        assertRejected(result, "File is too large")
    }

    @Test
    fun `parse rejects invalid utf8`() {
        val result = TextFileImportParser.parse(
            displayName = "broken.md",
            bytes = byteArrayOf(0xC3.toByte(), 0x28)
        )

        assertRejected(result, "Unsupported file encoding")
    }

    @Test
    fun `parse rejects binary control characters`() {
        val result = TextFileImportParser.parse(
            displayName = "binary.bin",
            bytes = byteArrayOf(0x00, 0x01, 0x02)
        )

        assertRejected(result, "File does not look like text")
    }

    private fun assertRejected(
        result: TextFileImportParseResult,
        expectedMessage: String
    ) {
        assertTrue(result is TextFileImportParseResult.Rejected)
        assertEquals(expectedMessage, (result as TextFileImportParseResult.Rejected).message)
    }
}

