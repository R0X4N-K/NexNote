package io.github.r0x4nk.nexnote.data.security

import com.google.crypto.tink.subtle.AesGcmHkdfStreaming
import io.github.r0x4nk.nexnote.util.copyStreaming
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import javax.crypto.SecretKey

/**
 * Version 2 uses Tink's segmented AEAD, including segment ordering and authenticated EOF.
 * Parameters are fixed by the version, and the envelope header is authenticated as AAD.
 * Only the legacy reader materializes a payload; all new file operations use fixed buffers.
 */
internal class VaultFileStreams(private val legacy: VaultFileCipher = VaultFileCipher()) {
    fun encrypt(input: InputStream, output: OutputStream, key: SecretKey, checkCancellation: () -> Unit = {}) {
        try {
            output.write(HEADER)
            // Closing the AEAD stream writes the final authenticated segment. The caller owns output.
            primitive(key).newEncryptingStream(object : FilterOutputStream(output) {
                override fun close() = flush()
                override fun write(bytes: ByteArray, offset: Int, length: Int) = out.write(bytes, offset, length)
            }, HEADER).use { encrypted -> copyStreaming(input, encrypted, checkCancellation) }
        } catch (error: GeneralSecurityException) {
            throw VaultEncryptionException("Vault file could not be encrypted.", error)
        }
    }

    /** Caller must consume through EOF before publishing output, and always close this stream. */
    fun decrypting(input: InputStream, key: SecretKey): InputStream {
        val source = input.buffered()
        try {
            source.mark(HEADER.size)
            val header = ByteArray(HEADER.size)
            var count = 0
            while (count < header.size) {
                val read = source.read(header, count, header.size - count)
                if (read < 0) break
                count += read
            }
            if (count == HEADER.size && header.contentEquals(HEADER)) {
                return object : FilterInputStream(primitive(key).newDecryptingStream(source, HEADER)) {
                    override fun read(): Int = authenticated { `in`.read() }
                    override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
                        authenticated { `in`.read(bytes, offset, length) }
                    private fun authenticated(read: () -> Int): Int = try { read() }
                    catch (error: IOException) {
                        throw VaultDecryptionException("Vault file could not be decrypted.", error)
                    }
                }
            }
            if (count < LEGACY_HEADER.size || !LEGACY_HEADER.indices.all { header[it] == LEGACY_HEADER[it] }) {
                throw VaultDecryptionException("Vault file envelope is unsupported.")
            }
            source.reset()
            val encoded = source.use { it.readBytes() }
            val plaintext = try { legacy.decryptToByteArray(encoded, key) } finally { encoded.fill(0) }
            return object : ByteArrayInputStream(plaintext) {
                override fun close() { plaintext.fill(0); super.close() }
            }
        } catch (error: Exception) {
            source.close()
            if (error is VaultDecryptionException) throw error
            throw VaultDecryptionException("Vault file could not be decrypted.", error)
        }
    }

    private fun primitive(key: SecretKey): AesGcmHkdfStreaming {
        val material = key.encoded ?: throw GeneralSecurityException("Vault key is unavailable")
        return try { AesGcmHkdfStreaming(material, "HmacSha256", 32, 64 * 1024, 0) }
        finally { material.fill(0) }
    }

    companion object {
        private val LEGACY_HEADER = "nexnote-vault-file:1:AES/GCM/NoPadding:".toByteArray(Charsets.US_ASCII)
        private val HEADER = "nexnote-vault-file:2:tink-aes256-gcm-hkdf-64k:".toByteArray(Charsets.US_ASCII)
        fun isStreaming(value: ByteArray): Boolean = value.size >= HEADER.size &&
            HEADER.indices.all { value[it] == HEADER[it] }
    }
}
