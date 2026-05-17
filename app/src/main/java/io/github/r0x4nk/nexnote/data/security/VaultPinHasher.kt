package io.github.r0x4nk.nexnote.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class VaultPinHash(
    val algorithm: String,
    val iterations: Int,
    val saltBase64: String,
    val hashBase64: String
)

class VaultPinHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val saltLengthBytes: Int = DEFAULT_SALT_LENGTH_BYTES,
    private val keyLengthBits: Int = DEFAULT_KEY_LENGTH_BITS
) {
    fun hash(pin: CharArray): VaultPinHash {
        require(pin.isNotEmpty()) { "Vault PIN must not be empty." }

        val salt = ByteArray(saltLengthBytes)
        secureRandom.nextBytes(salt)
        val derived = derive(pin, salt, iterations)

        return try {
            VaultPinHash(
                algorithm = ALGORITHM,
                iterations = iterations,
                saltBase64 = encode(salt),
                hashBase64 = encode(derived)
            )
        } finally {
            derived.fill(0)
        }
    }

    fun verify(pin: CharArray, stored: VaultPinHash): Boolean {
        if (pin.isEmpty() || stored.algorithm != ALGORITHM || stored.iterations <= 0) {
            return false
        }

        val salt = runCatching { decode(stored.saltBase64) }.getOrNull() ?: return false
        val expected = runCatching { decode(stored.hashBase64) }.getOrNull() ?: return false
        val candidate = derive(pin, salt, stored.iterations)

        return try {
            MessageDigest.isEqual(expected, candidate)
        } finally {
            candidate.fill(0)
            expected.fill(0)
        }
    }

    private fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, keyLengthBits)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray =
        Base64.getDecoder().decode(value)

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 310_000
        const val DEFAULT_SALT_LENGTH_BYTES = 16
        const val DEFAULT_KEY_LENGTH_BITS = 256
    }
}
