package io.github.r0x4nk.nexnote.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultPinHasherTest {

    private val hasher = VaultPinHasher(iterations = TEST_ITERATIONS)

    @Test
    fun `hash verifies the original pin`() {
        val stored = hasher.hash("1234".toCharArray())

        assertTrue(hasher.verify("1234".toCharArray(), stored))
    }

    @Test
    fun `hash rejects a different pin`() {
        val stored = hasher.hash("1234".toCharArray())

        assertFalse(hasher.verify("0000".toCharArray(), stored))
    }

    @Test
    fun `hash output does not store the pin in clear`() {
        val stored = hasher.hash("1234".toCharArray())

        assertNotEquals("1234", stored.hashBase64)
        assertNotEquals("1234", stored.saltBase64)
    }

    @Test
    fun `same pin receives different salts`() {
        val first = hasher.hash("1234".toCharArray())
        val second = hasher.hash("1234".toCharArray())

        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.hashBase64, second.hashBase64)
    }

    companion object {
        private const val TEST_ITERATIONS = 1_000
    }
}
