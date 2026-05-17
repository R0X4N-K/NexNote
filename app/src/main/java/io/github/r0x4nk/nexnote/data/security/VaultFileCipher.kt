package io.github.r0x4nk.nexnote.data.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val FILE_ENVELOPE_PREFIX = "nexnote-vault-file"
private const val FILE_ENVELOPE_SEPARATOR = ':'
private const val FILE_ENVELOPE_PART_COUNT = 5
private const val FILE_ENVELOPE_VERSION = 1
private const val FILE_IV_LENGTH_BYTES = 12
private const val FILE_TAG_LENGTH_BITS = 128

data class VaultEncryptedFile(
    val version: Int,
    val algorithm: String,
    val ivBase64: String,
    val ciphertextBase64: String
) {
    fun encodeToByteArray(): ByteArray =
        listOf(FILE_ENVELOPE_PREFIX, version.toString(), algorithm, ivBase64, ciphertextBase64)
            .joinToString(FILE_ENVELOPE_SEPARATOR.toString())
            .toByteArray(Charsets.UTF_8)

    companion object {
        fun decode(value: ByteArray): VaultEncryptedFile {
            val parts = String(value, Charsets.UTF_8)
                .split(FILE_ENVELOPE_SEPARATOR, limit = FILE_ENVELOPE_PART_COUNT)
            if (parts.size != FILE_ENVELOPE_PART_COUNT || parts[0] != FILE_ENVELOPE_PREFIX) {
                throw VaultDecryptionException("Vault file envelope is invalid.")
            }

            val version = parts[1].toIntOrNull()
                ?: throw VaultDecryptionException("Vault file envelope is invalid.")
            val algorithm = parts[2]
            val ivBase64 = parts[3]
            val ciphertextBase64 = parts[4]

            if (algorithm.isBlank() || ivBase64.isBlank() || ciphertextBase64.isBlank()) {
                throw VaultDecryptionException("Vault file envelope is invalid.")
            }

            return VaultEncryptedFile(
                version = version,
                algorithm = algorithm,
                ivBase64 = ivBase64,
                ciphertextBase64 = ciphertextBase64
            )
        }

        fun isEncoded(value: ByteArray): Boolean {
            val prefix = "$FILE_ENVELOPE_PREFIX$FILE_ENVELOPE_SEPARATOR"
                .toByteArray(Charsets.UTF_8)
            return value.size >= prefix.size &&
                prefix.indices.all { index -> value[index] == prefix[index] }
        }
    }
}

class VaultFileCipher(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    fun encryptToByteArray(plainBytes: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(FILE_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        val plainCopy = plainBytes.copyOf()
        var ciphertext = ByteArray(0)

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(FILE_TAG_LENGTH_BITS, iv))
            ciphertext = cipher.doFinal(plainCopy)

            VaultEncryptedFile(
                version = FILE_ENVELOPE_VERSION,
                algorithm = ALGORITHM,
                ivBase64 = encode(iv),
                ciphertextBase64 = encode(ciphertext)
            ).encodeToByteArray()
        } catch (error: GeneralSecurityException) {
            throw VaultEncryptionException("Vault file could not be encrypted.", error)
        } finally {
            plainCopy.fill(0)
            ciphertext.fill(0)
            iv.fill(0)
        }
    }

    fun decryptToByteArray(encrypted: ByteArray, key: SecretKey): ByteArray {
        val file = VaultEncryptedFile.decode(encrypted)
        if (file.version != FILE_ENVELOPE_VERSION || file.algorithm != ALGORITHM) {
            throw VaultDecryptionException("Vault file envelope is unsupported.")
        }

        val iv = decode(file.ivBase64)
        val ciphertext = decode(file.ciphertextBase64)
        var plainBytes = ByteArray(0)

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(FILE_TAG_LENGTH_BITS, iv))
            plainBytes = cipher.doFinal(ciphertext)
            plainBytes.copyOf()
        } catch (error: GeneralSecurityException) {
            throw VaultDecryptionException("Vault file could not be decrypted.", error)
        } finally {
            plainBytes.fill(0)
            ciphertext.fill(0)
            iv.fill(0)
        }
    }

    fun isEncryptedPayload(value: ByteArray): Boolean =
        VaultEncryptedFile.isEncoded(value)

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray =
        try {
            Base64.getDecoder().decode(value)
        } catch (error: IllegalArgumentException) {
            throw VaultDecryptionException("Vault file envelope is invalid.", error)
        }

    companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
    }
}
