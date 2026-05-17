package io.github.r0x4nk.nexnote.data.security

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultFieldCipherTest {

    private val cipher = VaultFieldCipher()
    private val key = SecretKeySpec(
        ByteArray(KEY_SIZE_BYTES) { index -> (index + 1).toByte() },
        KEY_ALGORITHM
    )
    private val differentKey = SecretKeySpec(
        ByteArray(KEY_SIZE_BYTES) { index -> (index + 65).toByte() },
        KEY_ALGORITHM
    )

    @Test
    fun `encryptToString returns an envelope without plaintext`() {
        val plaintext = "Private title\n#sensitive-tag\nBody text"

        val encrypted = cipher.encryptToString(plaintext, key)

        assertTrue(cipher.isEncryptedPayload(encrypted))
        assertFalse(encrypted.contains(plaintext))
        assertFalse(encrypted.contains("Private title"))
        assertFalse(encrypted.contains("#sensitive-tag"))
    }

    @Test
    fun `decryptToString restores encrypted text with the same key`() {
        val plaintext = "Line 1\nLine 2 with accented text: citta"

        val encrypted = cipher.encryptToString(plaintext, key)

        assertEquals(plaintext, cipher.decryptToString(encrypted, key))
    }

    @Test
    fun `same plaintext encrypts to different envelopes`() {
        val plaintext = "same protected field"

        val first = cipher.encryptToString(plaintext, key)
        val second = cipher.encryptToString(plaintext, key)

        assertFalse(first == second)
        assertEquals(plaintext, cipher.decryptToString(first, key))
        assertEquals(plaintext, cipher.decryptToString(second, key))
    }

    @Test
    fun `decryptToString rejects a different key`() {
        val encrypted = cipher.encryptToString("secret", key)

        assertThrowsVaultDecryption {
            cipher.decryptToString(encrypted, differentKey)
        }
    }

    @Test
    fun `decryptToString rejects a tampered envelope`() {
        val encrypted = cipher.encryptToString("secret", key)
        val parts = encrypted.split(':').toMutableList()
        parts[4] = parts[4].replaceFirstChar { char -> if (char == 'A') 'B' else 'A' }
        val tampered = parts.joinToString(":")

        assertThrowsVaultDecryption {
            cipher.decryptToString(tampered, key)
        }
    }

    @Test
    fun `decryptToString rejects non vault payloads`() {
        assertFalse(cipher.isEncryptedPayload("plain text"))

        assertThrowsVaultDecryption {
            cipher.decryptToString("plain text", key)
        }
    }

    private fun assertThrowsVaultDecryption(block: () -> Unit) {
        val result = runCatching(block)

        assertTrue(result.exceptionOrNull() is VaultDecryptionException)
    }

    companion object {
        private const val KEY_SIZE_BYTES = 32
        private const val KEY_ALGORITHM = "AES"
    }
}
