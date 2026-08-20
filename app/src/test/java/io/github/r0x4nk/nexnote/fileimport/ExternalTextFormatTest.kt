package io.github.r0x4nk.nexnote.fileimport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalTextFormatTest {

    @Test
    fun `accepts supported text and structured MIME types`() {
        assertTrue(ExternalTextFormat.isSupported("text/plain", "note.bin"))
        assertTrue(ExternalTextFormat.isSupported("text/markdown; charset=utf-8", "note"))
        assertTrue(ExternalTextFormat.isSupported("application/json", "payload"))
        assertTrue(ExternalTextFormat.isSupported("application/x-yaml", "payload"))
    }

    @Test
    fun `accepts generic MIME only for supported extension`() {
        assertTrue(ExternalTextFormat.isSupported("application/octet-stream", "NOTE.MD"))
        assertTrue(ExternalTextFormat.isSupported(null, "config.toml"))
        assertFalse(ExternalTextFormat.isSupported("application/octet-stream", "archive.zip"))
        assertFalse(ExternalTextFormat.isSupported(null, "no-extension"))
    }

    @Test
    fun `rejects unrelated MIME even when name looks textual`() {
        assertFalse(ExternalTextFormat.isSupported("image/png", "renamed.txt"))
        assertFalse(ExternalTextFormat.isSupported("application/pdf", "document.md"))
    }
}
