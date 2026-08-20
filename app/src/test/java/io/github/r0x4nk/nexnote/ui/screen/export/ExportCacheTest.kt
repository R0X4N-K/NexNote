package io.github.r0x4nk.nexnote.ui.screen.export

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExportCacheTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `same preferred name produces unique cache paths`() {
        val tokens = ArrayDeque(listOf("first", "second"))
        val cache = ExportCache(
            cacheDir = temporaryFolder.root,
            nowMillis = { 10_000L },
            uniqueToken = { tokens.removeFirst() }
        )

        val first = cache.prepareFile("Note.md")
        val second = cache.prepareFile("Note.md")

        assertNotEquals(first, second)
        assertEquals(cache.directory.canonicalFile, first.parentFile)
        assertEquals("md", first.extension)
    }

    @Test
    fun `equivalent non-canonical cache path remains confined and usable`() {
        val nested = File(temporaryFolder.root, "nested").apply { mkdirs() }
        val equivalentRoot = File(nested, "..")
        val cache = ExportCache(
            cacheDir = equivalentRoot,
            nowMillis = { 10_000L },
            uniqueToken = { "stable" }
        )

        val prepared = cache.prepareFile("Note.md")

        assertEquals(temporaryFolder.root.canonicalFile, prepared.parentFile?.parentFile)
        assertEquals(cache.directory.canonicalFile, prepared.parentFile)
    }

    @Test
    fun `cleanup removes expired files and retains recent files and directories`() {
        val now = ExportCache.RETENTION_MILLIS * 2
        val cache = ExportCache(temporaryFolder.root, nowMillis = { now })
        cache.directory.mkdirs()
        val expired = File(cache.directory, "expired.txt").apply {
            writeText("old")
            setLastModified(now - ExportCache.RETENTION_MILLIS)
        }
        val recent = File(cache.directory, "recent.txt").apply {
            writeText("new")
            setLastModified(now - ExportCache.RETENTION_MILLIS + 1)
        }
        val nested = File(cache.directory, "nested").apply { mkdirs() }

        assertEquals(1, cache.cleanupExpired())

        assertFalse(expired.exists())
        assertTrue(recent.exists())
        assertTrue(nested.isDirectory)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `path-like preferred name is rejected`() {
        ExportCache(temporaryFolder.root).prepareFile("../outside.txt")
    }
}
