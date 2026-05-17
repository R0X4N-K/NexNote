package io.github.r0x4nk.nexnote.data.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ENVELOPE_PREFIX = "nexnote-vault-field"
private const val ENVELOPE_SEPARATOR = ':'
private const val ENVELOPE_PART_COUNT = 5
private const val ENVELOPE_VERSION = 1
private const val IV_LENGTH_BYTES = 12
private const val TAG_LENGTH_BITS = 128

data class VaultEncryptedField(
    val version: Int,
    val algorithm: String,
    val ivBase64: String,
    val ciphertextBase64: String
) {
    fun encode(): String =
        listOf(ENVELOPE_PREFIX, version.toString(), algorithm, ivBase64, ciphertextBase64)
            .joinToString(ENVELOPE_SEPARATOR.toString())

    companion object {
        fun decode(value: String): VaultEncryptedField {
            val parts = value.split(ENVELOPE_SEPARATOR, limit = ENVELOPE_PART_COUNT)
            if (parts.size != ENVELOPE_PART_COUNT || parts[0] != ENVELOPE_PREFIX) {
                throw VaultDecryptionException("Vault field envelope is invalid.")
            }

            val version = parts[1].toIntOrNull()
                ?: throw VaultDecryptionException("Vault field envelope is invalid.")
            val algorithm = parts[2]
            val ivBase64 = parts[3]
            val ciphertextBase64 = parts[4]

            if (algorithm.isBlank() || ivBase64.isBlank() || ciphertextBase64.isBlank()) {
                throw VaultDecryptionException("Vault field envelope is invalid.")
            }

            return VaultEncryptedField(
                version = version,
                algorithm = algorithm,
                ivBase64 = ivBase64,
                ciphertextBase64 = ciphertextBase64
            )
        }
    }
}

class VaultFieldCipher(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    fun encryptToString(plainText: String, key: SecretKey): String {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)
        var ciphertext = ByteArray(0)

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            ciphertext = cipher.doFinal(plainBytes)

            VaultEncryptedField(
                version = ENVELOPE_VERSION,
                algorithm = ALGORITHM,
                ivBase64 = encode(iv),
                ciphertextBase64 = encode(ciphertext)
            ).encode()
        } catch (error: GeneralSecurityException) {
            throw VaultEncryptionException("Vault field could not be encrypted.", error)
        } finally {
            plainBytes.fill(0)
            ciphertext.fill(0)
            iv.fill(0)
        }
    }

    fun decryptToString(encrypted: String, key: SecretKey): String {
        val field = VaultEncryptedField.decode(encrypted)
        if (field.version != ENVELOPE_VERSION || field.algorithm != ALGORITHM) {
            throw VaultDecryptionException("Vault field envelope is unsupported.")
        }

        val iv = decode(field.ivBase64)
        val ciphertext = decode(field.ciphertextBase64)
        var plainBytes = ByteArray(0)

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            plainBytes = cipher.doFinal(ciphertext)
            String(plainBytes, Charsets.UTF_8)
        } catch (error: GeneralSecurityException) {
            throw VaultDecryptionException("Vault field could not be decrypted.", error)
        } finally {
            plainBytes.fill(0)
            ciphertext.fill(0)
            iv.fill(0)
        }
    }

    fun isEncryptedPayload(value: String): Boolean =
        value.startsWith("$ENVELOPE_PREFIX$ENVELOPE_SEPARATOR")

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray =
        try {
            Base64.getDecoder().decode(value)
        } catch (error: IllegalArgumentException) {
            throw VaultDecryptionException("Vault field envelope is invalid.", error)
        }

    companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
    }
}

class VaultEncryptionException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class VaultDecryptionException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
