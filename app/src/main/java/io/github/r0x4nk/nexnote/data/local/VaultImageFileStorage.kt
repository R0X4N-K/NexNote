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
 * Opaque rollback token for a Vault image rewrap.
 *
 * [backupFile] contains the original encrypted payload, never plaintext. The
 * token is confined to the data layer and must be either committed (backup
 * deleted) or rolled back (backup atomically restored over [targetFile]).
 */
internal data class VaultImageFileRewrapBackup(
    val targetFile: File,
    val backupFile: File
)

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

    /**
     * Re-encrypt one existing Vault image from [currentKey] to [newKey].
     *
     * The old ciphertext is copied to a private sibling backup before the new
     * ciphertext replaces the target. Plaintext exists only in memory and is
     * zeroed before returning. Missing files need no rollback token.
     */
    open suspend fun rewrapInPlace(
        relativePath: String,
        currentKey: SecretKey,
        newKey: SecretKey,
        onBackupCreated: (VaultImageFileRewrapBackup) -> Unit = {}
    ): VaultImageFileRewrapBackup? = withContext(ioDispatcher) {
        val target = imageStorage.getImageFile(relativePath)
        if (!target.isFile) return@withContext null

        val oldCiphertext = target.readBytes()
        var plaintext = ByteArray(0)
        var newCiphertext = ByteArray(0)
        var backup: File? = null
        var replaced = false

        try {
            if (!fileCipher.isEncryptedPayload(oldCiphertext)) {
                throw IOException("Vault image payload is not encrypted.")
            }
            plaintext = fileCipher.decryptToByteArray(oldCiphertext, currentKey)
            newCiphertext = fileCipher.encryptToByteArray(plaintext, newKey)

            val parent = target.parentFile
                ?: throw IOException("Vault image parent is unavailable.")
            backup = File.createTempFile(".${target.name}.rekey-old-", ".tmp", parent)
            backup.writeBytes(oldCiphertext)
            replaceFileConservatively(target, newCiphertext)
            replaced = true
            VaultImageFileRewrapBackup(targetFile = target, backupFile = backup).also(
                onBackupCreated
            )
        } finally {
            oldCiphertext.fill(0)
            plaintext.fill(0)
            newCiphertext.fill(0)
            if (!replaced) {
                backup?.delete()
            }
        }
    }

    /** Restore the original encrypted payload represented by [backup]. */
    open suspend fun rollbackRewrap(
        backup: VaultImageFileRewrapBackup
    ) = withContext(ioDispatcher) {
        if (!backup.backupFile.isFile) {
            throw IOException("Vault image rewrap backup is missing.")
        }
        val oldCiphertext = backup.backupFile.readBytes()
        try {
            replaceFileConservatively(backup.targetFile, oldCiphertext)
        } finally {
            oldCiphertext.fill(0)
        }
    }

    /** Delete the no-longer-needed old ciphertext after the PIN commit. */
    open suspend fun commitRewrap(
        backup: VaultImageFileRewrapBackup
    ) = withContext(ioDispatcher) {
        if (backup.backupFile.exists() && !backup.backupFile.delete()) {
            throw IOException("Vault image rewrap backup could not be removed.")
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
