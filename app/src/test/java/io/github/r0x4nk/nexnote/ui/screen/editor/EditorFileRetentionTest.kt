package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorFileRetentionTest {

    @Test
    fun `returns owned paths that are no longer referenced`() {
        val orphans = unreferencedStoredPaths(
            content = "![photo](images/kept.jpg)",
            ownedPaths = listOf("images/kept.jpg", "images/removed.pdf")
        )

        assertEquals(listOf("images/removed.pdf"), orphans)
    }

    @Test
    fun `keeps paths that still appear anywhere in the content`() {
        val orphans = unreferencedStoredPaths(
            content = "See images/legacy.png for context",
            ownedPaths = listOf("images/legacy.png")
        )

        assertEquals(emptyList<String>(), orphans)
    }

    @Test
    fun `ignores blank manifest entries`() {
        val orphans = unreferencedStoredPaths(
            content = "",
            ownedPaths = listOf("", "images/removed.jpg")
        )

        assertEquals(listOf("images/removed.jpg"), orphans)
    }

    @Test
    fun `returns nothing when every owned path is referenced`() {
        val orphans = unreferencedStoredPaths(
            content = "[Report](images/report.pdf) and ![img](images/img.png)",
            ownedPaths = listOf("images/report.pdf", "images/img.png")
        )

        assertEquals(emptyList<String>(), orphans)
    }
}
