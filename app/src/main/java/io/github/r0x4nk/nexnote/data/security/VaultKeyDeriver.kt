package io.github.r0x4nk.nexnote.data.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class VaultKeyDerivationParams(
    val algorithm: String,
    val iterations: Int,
    val saltBase64: String,
    val keyLengthBits: Int
)

class VaultKeyDeriver(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val saltLengthBytes: Int = DEFAULT_SALT_LENGTH_BYTES,
    private val keyLengthBits: Int = DEFAULT_KEY_LENGTH_BITS
) {
    fun newParams(): VaultKeyDerivationParams {
        if (iterations <= 0 || saltLengthBytes <= 0 || keyLengthBits !in SUPPORTED_AES_KEY_LENGTHS) {
            throw VaultKeyDerivationException("Vault key derivation params are invalid.")
        }

        val salt = ByteArray(saltLengthBytes)
        secureRandom.nextBytes(salt)

        return try {
            VaultKeyDerivationParams(
                algorithm = ALGORITHM,
                iterations = iterations,
                saltBase64 = encode(salt),
                keyLengthBits = keyLengthBits
            )
        } finally {
            salt.fill(0)
        }
    }

    fun deriveKey(pin: CharArray, params: VaultKeyDerivationParams): SecretKey {
        if (pin.isEmpty()) {
            throw VaultKeyDerivationException("Vault key derivation input is invalid.")
        }
        params.validate()

        val salt = decode(params.saltBase64)
        val spec = PBEKeySpec(pin, salt, params.iterations, params.keyLengthBits)
        var keyBytes = ByteArray(0)

        return try {
            keyBytes = SecretKeyFactory
                .getInstance(params.algorithm)
                .generateSecret(spec)
                .encoded

            SecretKeySpec(keyBytes, KEY_ALGORITHM)
        } catch (error: GeneralSecurityException) {
            throw VaultKeyDerivationException("Vault key could not be derived.", error)
        } finally {
            spec.clearPassword()
            keyBytes.fill(0)
            salt.fill(0)
        }
    }

    private fun VaultKeyDerivationParams.validate() {
        if (
            algorithm != ALGORITHM ||
            iterations <= 0 ||
            saltBase64.isBlank() ||
            keyLengthBits !in SUPPORTED_AES_KEY_LENGTHS
        ) {
            throw VaultKeyDerivationException("Vault key derivation params are unsupported.")
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray =
        try {
            Base64.getDecoder().decode(value)
        } catch (error: IllegalArgumentException) {
            throw VaultKeyDerivationException("Vault key derivation params are invalid.", error)
        }

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val KEY_ALGORITHM = "AES"
        const val DEFAULT_ITERATIONS = 310_000
        const val DEFAULT_SALT_LENGTH_BYTES = 16
        const val DEFAULT_KEY_LENGTH_BITS = 256

        private val SUPPORTED_AES_KEY_LENGTHS = setOf(128, 192, 256)
    }
}

class VaultKeyDerivationException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
