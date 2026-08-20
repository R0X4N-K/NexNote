package io.github.r0x4nk.nexnote.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedStreamCopyTest {

    @Test
    fun `copyBounded copies input at exact limit`() {
        val bytes = ByteArray(32) { it.toByte() }
        val output = ByteArrayOutputStream()

        val copied = copyBounded(ByteArrayInputStream(bytes), output, bytes.size.toLong())

        assertEquals(bytes.size.toLong(), copied)
        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test
    fun `copyBounded rejects oversize input before writing beyond limit`() {
        val output = ByteArrayOutputStream()

        val thrown = runCatching {
            copyBounded(ByteArrayInputStream(ByteArray(33)), output, 32L)
        }.exceptionOrNull()

        assertTrue(thrown is InputTooLargeException)
        assertTrue(output.size() <= 32)
    }
}
