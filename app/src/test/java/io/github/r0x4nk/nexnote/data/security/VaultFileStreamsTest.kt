package io.github.r0x4nk.nexnote.data.security

import io.github.r0x4nk.nexnote.util.copyStreaming
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultFileStreamsTest {
    @get:Rule val folder = TemporaryFolder()
    private val streams = VaultFileStreams()
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val otherKey = SecretKeySpec(ByteArray(32) { (it + 9).toByte() }, "AES")

    @Test fun `round trips empty short and multi segment files`() {
        for (size in listOf(0, 1, 65_479, 65_480, 65_536, 150_000)) {
            val plain = ByteArray(size) { (it % 251).toByte() }
            val encoded = encrypt(plain)
            assertTrue(VaultEncryptedFile.isEncoded(encoded))
            assertArrayEquals(plain, decrypt(encoded))
            assertFalse(encoded.contentEquals(encrypt(plain)))
        }
    }

    @Test fun `rejects wrong keys truncation append tampering and segment reordering`() {
        val encoded = encrypt(ByteArray(220_000) { it.toByte() })
        assertTrue(runCatching { streams.decrypting(encoded.inputStream(), otherKey).use { it.readBytes() } }.isFailure)
        val header = "nexnote-vault-file:2:tink-aes256-gcm-hkdf-64k:".length
        val variants = listOf(
            encoded.copyOf(12), encoded.copyOf(encoded.size - 1),
            encoded.copyOf(header + 65_536), encoded + byteArrayOf(0),
            encoded.copyOf().also { it[header + 100] = (it[header + 100].toInt() xor 1).toByte() },
            encoded.copyOf().also {
                val segment = encoded.copyOfRange(header + 65_536, header + 131_072)
                encoded.copyInto(it, header + 65_536, header + 131_072, header + 196_608)
                segment.copyInto(it, header + 131_072)
            }
        )
        variants.forEach { assertTrue(runCatching { decrypt(it) }.isFailure) }
    }

    @Test fun `legacy payload remains readable and reencrypts as streaming`() {
        val plain = "legacy document".toByteArray()
        val legacy = VaultFileCipher().encryptToByteArray(plain, key)
        assertArrayEquals(plain, decrypt(legacy))
        val output = ByteArrayOutputStream()
        streams.decrypting(legacy.inputStream(), key).use { streams.encrypt(it, output, otherKey) }
        assertTrue(VaultFileStreams.isStreaming(output.toByteArray()))
        assertArrayEquals(plain, streams.decrypting(output.toByteArray().inputStream(), otherKey).use { it.readBytes() })
    }

    @Test fun `128 MiB file encrypts decrypts and rekeys with fixed working buffers`() {
        val size = 128L * 1024 * 1024
        val original = folder.newFile()
        val rekeyed = folder.newFile()
        original.outputStream().use { streams.encrypt(GeneratedInput(size), it, key) }
        streams.decrypting(original.inputStream(), key).use { input ->
            rekeyed.outputStream().use { streams.encrypt(input, it, otherKey) }
        }
        var total = 0L
        val verifier = object : OutputStream() {
            override fun write(value: Int) { assertEquals((total++ % 251).toInt(), value and 255) }
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                assertTrue(length <= 8192)
                for (i in offset until offset + length) {
                    if ((bytes[i].toInt() and 255) != (total % 251).toInt()) fail("Byte mismatch at $total")
                    total++
                }
            }
        }
        streams.decrypting(rekeyed.inputStream(), otherKey).use { copyStreaming(it, verifier) }
        assertEquals(size, total)
    }

    private fun encrypt(plain: ByteArray): ByteArray = ByteArrayOutputStream().also {
        streams.encrypt(plain.inputStream(), it, key)
    }.toByteArray()
    private fun decrypt(encoded: ByteArray): ByteArray = streams.decrypting(encoded.inputStream(), key).use { it.readBytes() }

    private class GeneratedInput(private val size: Long) : InputStream() {
        private var position = 0L
        override fun read(): Int = if (position == size) -1 else (position++ % 251).toInt()
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            if (position == size) return -1
            assertTrue(length <= 8192)
            val count = minOf(length.toLong(), size - position).toInt()
            for (i in offset until offset + count) bytes[i] = (position++ % 251).toByte()
            return count
        }
    }
}
