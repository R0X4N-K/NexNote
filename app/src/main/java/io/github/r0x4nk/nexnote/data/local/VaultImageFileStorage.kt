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

internal sealed interface VaultImageFileRestoreResult {
    data object Restored : VaultImageFileRestoreResult
    data object AlreadyPlaintext : VaultImageFileRestoreResult
    data object Missing : VaultImageFileRestoreResult
}

/**
 * File boundary for Vault image payloads stored through [NoteImageStorage].
 *
 * This component intentionally does not update note rows or UI state. It only
 * transforms the bytes of an already-known internal image path.
 */
internal open class VaultImageFileStorage(
    private val imageStorage: NoteImageStorage,
    private val fileCipher: VaultFileCipher = VaultFileCipher(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    open suspend fun encryptInPlace(
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

    open suspend fun decryptToByteArray(
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

    /**
     * Restore an encrypted Vault image file to plaintext in place.
     *
     * This is intentionally narrow: it exists for rollback paths where a note
     * move into the Vault failed before the database row was converted, and for
     * controlled "remove from Vault" flows where the database row has already
     * been converted back to a normal note. Normal Vault reads should use
     * [decryptToByteArray] and keep plaintext in memory.
     */
    open suspend fun decryptInPlace(
        relativePath: String,
        key: SecretKey
    ): VaultImageFileRestoreResult = withContext(ioDispatcher) {
        val file = imageStorage.getImageFile(relativePath)
        if (!file.isFile) return@withContext VaultImageFileRestoreResult.Missing

        val encryptedOrPlainBytes = file.readBytes()
        var plainBytes = ByteArray(0)

        try {
            if (!fileCipher.isEncryptedPayload(encryptedOrPlainBytes)) {
                return@withContext VaultImageFileRestoreResult.AlreadyPlaintext
            }

            plainBytes = fileCipher.decryptToByteArray(encryptedOrPlainBytes, key)
            replaceFileConservatively(file, plainBytes)
            VaultImageFileRestoreResult.Restored
        } finally {
            encryptedOrPlainBytes.fill(0)
            plainBytes.fill(0)
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
