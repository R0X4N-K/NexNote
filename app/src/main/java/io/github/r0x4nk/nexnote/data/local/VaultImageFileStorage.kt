package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.data.security.VaultFileCipher
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface VaultImageFileEncryptionResult {
    data object Encrypted : VaultImageFileEncryptionResult
    data object AlreadyEncrypted : VaultImageFileEncryptionResult
    data object Missing : VaultImageFileEncryptionResult
}

internal sealed interface VaultImageFileDecryptionResult {
    class Decrypted(val bytes: ByteArray) : VaultImageFileDecryptionResult
    data object Missing : VaultImageFileDecryptionResult
}

/**
 * File boundary for Vault image payloads stored through [NoteImageStorage].
 *
 * This component intentionally does not update note rows or UI state. It only
 * transforms the bytes of an already-known internal image path.
 */
internal class VaultImageFileStorage(
    private val imageStorage: NoteImageStorage,
    private val fileCipher: VaultFileCipher = VaultFileCipher(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun encryptInPlace(
        relativePath: String,
        key: SecretKey
    ): VaultImageFileEncryptionResult = withContext(ioDispatcher) {
        val file = imageStorage.getImageFile(relativePath)
        if (!file.isFile) return@withContext VaultImageFileEncryptionResult.Missing

        val plainOrEncryptedBytes = file.readBytes()
        var encryptedBytes = ByteArray(0)

        try {
            if (fileCipher.isEncryptedPayload(plainOrEncryptedBytes)) {
                return@withContext VaultImageFileEncryptionResult.AlreadyEncrypted
            }

            encryptedBytes = fileCipher.encryptToByteArray(plainOrEncryptedBytes, key)
            replaceFileConservatively(file, encryptedBytes)
            VaultImageFileEncryptionResult.Encrypted
        } finally {
            plainOrEncryptedBytes.fill(0)
            encryptedBytes.fill(0)
        }
    }

    suspend fun decryptToByteArray(
        relativePath: String,
        key: SecretKey
    ): VaultImageFileDecryptionResult = withContext(ioDispatcher) {
        val file = imageStorage.getImageFile(relativePath)
        if (!file.isFile) return@withContext VaultImageFileDecryptionResult.Missing

        val encryptedBytes = file.readBytes()
        try {
            VaultImageFileDecryptionResult.Decrypted(
                fileCipher.decryptToByteArray(encryptedBytes, key)
            )
        } finally {
            encryptedBytes.fill(0)
        }
    }

    private fun replaceFileConservatively(target: File, replacementBytes: ByteArray) {
        val parent = target.parentFile
            ?: throw IOException("Vault image parent is unavailable.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Vault image parent is unavailable.")
        }

        val tempFile = File.createTempFile("${target.name}.vault-", ".tmp", parent)
        var replaced = false

        try {
            tempFile.writeBytes(replacementBytes)
            moveReplacing(tempFile, target)
            replaced = true
        } finally {
            if (!replaced) {
                tempFile.delete()
            }
        }
    }

    private fun moveReplacing(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}
