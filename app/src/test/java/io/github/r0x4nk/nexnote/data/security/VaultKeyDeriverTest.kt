package io.github.r0x4nk.nexnote.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultKeyDeriverTest {

    private val deriver = VaultKeyDeriver(iterations = TEST_ITERATIONS)
    private val cipher = VaultFieldCipher()

    @Test
    fun `newParams creates fresh non pin params`() {
        val first = deriver.newParams()
        val second = deriver.newParams()

        assertEquals(VaultKeyDeriver.ALGORITHM, first.algorithm)
        assertEquals(TEST_ITERATIONS, first.iterations)
        assertEquals(VaultKeyDeriver.DEFAULT_KEY_LENGTH_BITS, first.keyLengthBits)
        assertFalse(first.saltBase64.contains("1234"))
        assertNotEquals(first.saltBase64, second.saltBase64)
    }

    @Test
    fun `same pin and params derive a key that decrypts vault fields`() {
        val params = deriver.newParams()
        val plaintext = "Private title\n#secret\nBody"
        val encryptKey = deriver.deriveKey("1234".toCharArray(), params)
        val decryptKey = deriver.deriveKey("1234".toCharArray(), params)

        val encrypted = cipher.encryptToString(plaintext, encryptKey)

        assertEquals(plaintext, cipher.decryptToString(encrypted, decryptKey))
    }

    @Test
    fun `different pin cannot decrypt fields encrypted with derived key`() {
        val params = deriver.newParams()
        val encrypted = cipher.encryptToString(
            plainText = "secret",
            key = deriver.deriveKey("1234".toCharArray(), params)
        )

        assertThrowsVaultDecryption {
            cipher.decryptToString(
                encrypted = encrypted,
                key = deriver.deriveKey("0000".toCharArray(), params)
            )
        }
    }

    @Test
    fun `same pin with different params cannot decrypt fields`() {
        val firstParams = deriver.newParams()
        val secondParams = deriver.newParams()
        val encrypted = cipher.encryptToString(
            plainText = "secret",
            key = deriver.deriveKey("1234".toCharArray(), firstParams)
        )

        assertThrowsVaultDecryption {
            cipher.decryptToString(
                encrypted = encrypted,
                key = deriver.deriveKey("1234".toCharArray(), secondParams)
            )
        }
    }

    @Test
    fun `deriveKey rejects invalid params`() {
        val params = VaultKeyDerivationParams(
            algorithm = "unsupported",
            iterations = TEST_ITERATIONS,
            saltBase64 = deriver.newParams().saltBase64,
            keyLengthBits = VaultKeyDeriver.DEFAULT_KEY_LENGTH_BITS
        )

        assertThrowsVaultKeyDerivation {
            deriver.deriveKey("1234".toCharArray(), params)
        }
    }

    @Test
    fun `deriveKey rejects empty pin`() {
        assertThrowsVaultKeyDerivation {
            deriver.deriveKey(charArrayOf(), deriver.newParams())
        }
    }

    private fun assertThrowsVaultDecryption(block: () -> Unit) {
        val result = runCatching(block)

        assertTrue(result.exceptionOrNull() is VaultDecryptionException)
    }

    private fun assertThrowsVaultKeyDerivation(block: () -> Unit) {
        val result = runCatching(block)

        assertTrue(result.exceptionOrNull() is VaultKeyDerivationException)
    }

    companion object {
        private const val TEST_ITERATIONS = 1_000
    }
}
