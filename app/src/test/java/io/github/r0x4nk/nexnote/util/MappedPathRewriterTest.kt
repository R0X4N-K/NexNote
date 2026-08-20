package io.github.r0x4nk.nexnote.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MappedPathRewriterTest {

    @Test
    fun `rewrites every mapped path`() {
        val content = "![one](images/one.jpg) ![two](images/two.jpg)"

        val rewritten = content.rewriteMappedPaths(
            mapOf(
                "images/one.jpg" to "images/copy-one.jpg",
                "images/two.jpg" to "images/copy-two.jpg"
            )
        )

        assertEquals(
            "![one](images/copy-one.jpg) ![two](images/copy-two.jpg)",
            rewritten
        )
    }

    @Test
    fun `does not cascade a replacement into another source`() {
        assertEquals(
            "copy-a copy-b",
            "source-a copy-a".rewriteMappedPaths(
                linkedMapOf(
                    "source-a" to "copy-a",
                    "copy-a" to "copy-b"
                )
            )
        )
    }

    @Test
    fun `matches a longer overlapping source first`() {
        assertEquals(
            "long-copy short-copy",
            "images/note.jpg.backup images/note.jpg".rewriteMappedPaths(
                mapOf(
                    "images/note.jpg" to "short-copy",
                    "images/note.jpg.backup" to "long-copy"
                )
            )
        )
    }
}
