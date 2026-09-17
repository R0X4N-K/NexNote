package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.data.security.VaultFileCipher
import io.github.r0x4nk.nexnote.data.security.VaultFileStreams
import io.github.r0x4nk.nexnote.data.security.VaultDecryptionException
import io.github.r0x4nk.nexnote.util.copyStreaming
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.ensureActive
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

        if (isEncrypted(file)) return@withContext VaultImageFileEncryptionResult.AlreadyEncrypted
        val context = coroutineContext
        replaceFileConservatively(file) { output ->
            file.inputStream().use { input -> streams.encrypt(input, output, key, context::ensureActive) }
        }
        VaultImageFileEncryptionResult.Encrypted
    }

    private val streams = VaultFileStreams(fileCipher)

    /** Only image decoding should materialize plaintext. Documents use [openDecryptedStream]. */
    open suspend fun decryptToByteArray(
        relativePath: String,
        key: SecretKey
    ): VaultImageFileDecryptionResult = withContext(ioDispatcher) {
        val input = openDecryptedStream(relativePath, key)
            ?: return@withContext VaultImageFileDecryptionResult.Missing
        try {
            VaultImageFileDecryptionResult.Decrypted(input.use { it.readBytes() })
        } catch (error: Exception) {
            throw VaultDecryptionException("Vault file could not be decrypted.", error)
        }
    }

    fun openDecryptedStream(relativePath: String, key: SecretKey): InputStream? {
        val file = imageStorage.getImageFile(relativePath)
        return if (file.isFile) streams.decrypting(file.inputStream(), key) else null
    }

    private fun isEncrypted(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(32)
        var size = 0
        while (size < header.size) {
            val read = input.read(header, size, header.size - size)
            if (read < 0) break
            size += read
        }
        fileCipher.isEncryptedPayload(header.copyOf(size))
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

        if (!isEncrypted(file)) return@withContext VaultImageFileRestoreResult.AlreadyPlaintext
        val context = coroutineContext
        replaceFileConservatively(file) { output ->
            streams.decrypting(file.inputStream(), key).use { input ->
                copyStreaming(input, output, context::ensureActive)
            }
        }
        VaultImageFileRestoreResult.Restored
    }

    /**
     * Re-encrypt one existing Vault image from [currentKey] to [newKey].
     *
     * The old ciphertext is copied to a private sibling backup before the new
     * ciphertext replaces the target. Plaintext passes through bounded in-memory
     * buffers and is never staged on disk during rekey. Missing files need no rollback token.
     */
    open suspend fun rewrapInPlace(
        relativePath: String,
        currentKey: SecretKey,
        newKey: SecretKey,
        onBackupCreated: (VaultImageFileRewrapBackup) -> Unit = {}
    ): VaultImageFileRewrapBackup? = withContext(ioDispatcher) {
        val target = imageStorage.getImageFile(relativePath)
        if (!target.isFile) return@withContext null

        if (!isEncrypted(target)) throw IOException("Vault file payload is not encrypted.")
        val parent = target.parentFile ?: throw IOException("Vault file parent is unavailable.")
        val backup = File.createTempFile(".${target.name}.rekey-old-", ".tmp", parent)
        var replaced = false
        val context = coroutineContext
        try {
            target.inputStream().use { input ->
                backup.outputStream().use { output ->
                    copyStreaming(input, output, context::ensureActive)
                    output.fd.sync()
                }
            }
            replaceFileConservatively(target) { output ->
                streams.decrypting(target.inputStream(), currentKey).use { input ->
                    streams.encrypt(input, output, newKey, context::ensureActive)
                }
            }
            replaced = true
            VaultImageFileRewrapBackup(target, backup).also(onBackupCreated)
        } finally {
            if (!replaced) backup.delete()
        }
    }

    /** Restore the exact original ciphertext without materializing it. */
    open suspend fun rollbackRewrap(backup: VaultImageFileRewrapBackup) = withContext(ioDispatcher) {
        if (!backup.backupFile.isFile) throw IOException("Vault file rewrap backup is missing.")
        val context = coroutineContext
        replaceFileConservatively(backup.targetFile) { output ->
            backup.backupFile.inputStream().use { input -> copyStreaming(input, output, context::ensureActive) }
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

    private fun replaceFileConservatively(target: File, writeReplacement: (OutputStream) -> Unit) {
        val parent = target.parentFile
            ?: throw IOException("Vault image parent is unavailable.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Vault image parent is unavailable.")
        }

        val tempFile = File.createTempFile("${target.name}.vault-", ".tmp", parent)
        var replaced = false

        try {
            tempFile.outputStream().use { output ->
                writeReplacement(output)
                output.fd.sync()
            }
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
