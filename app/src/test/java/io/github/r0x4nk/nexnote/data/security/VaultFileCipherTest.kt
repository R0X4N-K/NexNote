package io.github.r0x4nk.nexnote.data.security

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultFileCipherTest {

    private val cipher = VaultFileCipher()
    private val key = SecretKeySpec(
        ByteArray(KEY_SIZE_BYTES) { index -> (index + 1).toByte() },
        KEY_ALGORITHM
    )
    private val differentKey = SecretKeySpec(
        ByteArray(KEY_SIZE_BYTES) { index -> (index + 65).toByte() },
        KEY_ALGORITHM
    )

    @Test
    fun `encryptToByteArray returns an envelope without plaintext bytes`() {
        val plaintext = "RAW-PRIVATE-IMAGE-BYTES".toByteArray(Charsets.UTF_8)

        val encrypted = cipher.encryptToByteArray(plaintext, key)
        val encodedEnvelope = String(encrypted, Charsets.UTF_8)

        assertTrue(cipher.isEncryptedPayload(encrypted))
        assertFalse(encodedEnvelope.contains("RAW-PRIVATE-IMAGE-BYTES"))
        assertArrayEquals("caller-owned bytes are not mutated", plaintextFixture(), plaintext)
    }

    @Test
    fun `decryptToByteArray restores encrypted bytes with the same key`() {
        val plaintext = byteArrayOf(0, 1, 2, 3, 10, 127, -128, -1)

        val encrypted = cipher.encryptToByteArray(plaintext, key)
        val decrypted = cipher.decryptToByteArray(encrypted, key)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `same bytes encrypt to different envelopes`() {
        val plaintext = byteArrayOf(42, 43, 44, 45)

        val first = cipher.encryptToByteArray(plaintext, key)
        val second = cipher.encryptToByteArray(plaintext, key)

        assertFalse(first.contentEquals(second))
        assertArrayEquals(plaintext, cipher.decryptToByteArray(first, key))
        assertArrayEquals(plaintext, cipher.decryptToByteArray(second, key))
    }

    @Test
    fun `decryptToByteArray supports empty file bytes`() {
        val encrypted = cipher.encryptToByteArray(ByteArray(0), key)

        assertEquals(0, cipher.decryptToByteArray(encrypted, key).size)
    }

    @Test
    fun `decryptToByteArray rejects a different key`() {
        val encrypted = cipher.encryptToByteArray(byteArrayOf(1, 2, 3), key)

        assertThrowsVaultDecryption {
            cipher.decryptToByteArray(encrypted, differentKey)
        }
    }

    @Test
    fun `decryptToByteArray rejects a tampered envelope`() {
        val encrypted = cipher.encryptToByteArray(byteArrayOf(1, 2, 3), key)
        val parts = String(encrypted, Charsets.UTF_8).split(':').toMutableList()
        parts[4] = parts[4].replaceFirstChar { char -> if (char == 'A') 'B' else 'A' }
        val tampered = parts.joinToString(":").toByteArray(Charsets.UTF_8)

        assertThrowsVaultDecryption {
            cipher.decryptToByteArray(tampered, key)
        }
    }

    @Test
    fun `decryptToByteArray rejects non vault payloads`() {
        val plainPayload = byteArrayOf(1, 2, 3, 4)

        assertFalse(cipher.isEncryptedPayload(plainPayload))

        assertThrowsVaultDecryption {
            cipher.decryptToByteArray(plainPayload, key)
        }
    }

    @Test
    fun `decode rejects unsupported envelope metadata`() {
        val encrypted = String(cipher.encryptToByteArray(byteArrayOf(1), key), Charsets.UTF_8)
        val unsupportedVersion = encrypted.replaceFirst(
            "nexnote-vault-file:1:",
            "nexnote-vault-file:2:"
        ).toByteArray(Charsets.UTF_8)

        assertThrowsVaultDecryption {
            cipher.decryptToByteArray(unsupportedVersion, key)
        }
    }

    private fun assertThrowsVaultDecryption(block: () -> Unit) {
        val result = runCatching(block)

        assertTrue(result.exceptionOrNull() is VaultDecryptionException)
    }

    private fun plaintextFixture(): ByteArray =
        "RAW-PRIVATE-IMAGE-BYTES".toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_SIZE_BYTES = 32
        private const val KEY_ALGORITHM = "AES"
    }
}
