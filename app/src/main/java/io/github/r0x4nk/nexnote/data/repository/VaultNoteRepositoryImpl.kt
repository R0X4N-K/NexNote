package io.github.r0x4nk.nexnote.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.TagDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.data.local.VaultImageFileDecryptionResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileEncryptionResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileRestoreResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileStorage
import io.github.r0x4nk.nexnote.data.security.VaultFieldCipher
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.VaultLockedException
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.TagParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

/**
 * Internal data-layer boundary that re-encrypts every active Vault note with a
 * freshly derived key. Exposed only inside the data package so callers that
 * coordinate PIN changes can swap the key without leaking [SecretKey] into the
 * domain contracts. Implementations must require the Vault to be currently
 * unlocked: rewrap reads ciphertext using the existing key, decrypts it in
 * memory, and writes back ciphertext under the new key.
 */
internal interface VaultNoteRewrapper {
    /**
     * Re-encrypt all active Vault notes with [newKey]. The current Vault key
     * must already be unlocked, otherwise [VaultLockedException] is thrown
     * before any read or write happens. The operation is transactional and
     * does not alter `lastModifiedDate` or any non-encrypted metadata.
     */
    suspend fun rewrapAllVaultNotesWith(newKey: SecretKey)
}

/**
 * Internal data-layer boundary that hard-deletes every Vault note row. Exposed
 * only inside the data package so that the Vault reset coordinator can drop
 * encrypted payloads and their internal image files from one boundary.
 * Implementations must require the Vault to be currently unlocked before
 * decrypting stored image paths, and must leave normal notes (active or
 * trashed) untouched.
 */
internal interface VaultNoteWiper {
    /**
     * Hard-delete every Vault note row, including soft-deleted ones, remove
     * their associated internal image files when possible, and return the
     * number of rows removed. Implementations must not touch normal notes.
     */
    suspend fun wipeAllVaultNotes(): Int
}

/**
 * Explicit result for [VaultNoteImageEncryptor.encryptImagesForVaultNote]. The
 * sealed contract keeps callers from confusing "no note" with "note had no
 * images" and avoids leaking any path, title or content via exceptions.
 */
internal sealed interface VaultNoteImageEncryptionResult {
    /** Every referenced file was either encrypted in place or already encrypted. */
    data object Success : VaultNoteImageEncryptionResult

    /** The id does not match an active Vault note. */
    data object NoteNotFound : VaultNoteImageEncryptionResult

    /** The Vault note exists but does not reference any image file. */
    data object NoImages : VaultNoteImageEncryptionResult
}

/**
 * Internal data-layer boundary that encrypts in place the physical image files
 * referenced by an active Vault note, using [VaultImageFileStorage] and the
 * currently unlocked Vault key. The boundary intentionally does not modify the
 * encrypted note row, the note flow or any UI state: it only transforms the
 * bytes of files already referenced by a known Vault note id.
 *
 * Exposed only inside the data package so future steps can wire it to save,
 * move and editor flows without leaking the Vault [SecretKey] into the domain
 * contracts. Implementations must require the Vault to be currently unlocked.
 */
internal interface VaultNoteImageEncryptor {
    /**
     * Encrypt in place every image file referenced by the active Vault note
     * with [noteId]. Files that are already wrapped in a Vault file envelope
     * are left untouched. Missing files do not abort the operation and do not
     * flip the result to a failure: the boundary is best-effort over its file
     * inputs, but it always requires an unlocked Vault.
     *
     * @throws VaultLockedException if the Vault is locked when the call runs.
     */
    suspend fun encryptImagesForVaultNote(noteId: Long): VaultNoteImageEncryptionResult
}

internal class VaultNoteRepositoryImpl(
    private val database: RoomDatabase,
    private val dao: NoteDao,
    private val tagDao: TagDao,
    private val keyProvider: VaultUnlockedKeyProvider,
    private val imageStorage: NoteImageStorage,
    private val fieldCipher: VaultFieldCipher = VaultFieldCipher(),
    private val vaultImageFileStorage: VaultImageFileStorage =
        VaultImageFileStorage(imageStorage = imageStorage)
) : VaultNoteRepository, VaultNoteRewrapper, VaultNoteWiper, VaultNoteImageEncryptor {

    override val vaultNotes: Flow<List<Note>> =
        dao.getAllVaultNotes()
            .distinctUntilChanged()
            .map { entities ->
                keyProvider.withUnlockedVaultKey { key ->
                    entities.map { entity -> entity.toDecryptedDomain(key) }
                } ?: emptyList()
            }

    override suspend fun getVaultNoteById(id: Long): Note? =
        keyProvider.withUnlockedVaultKey { key ->
            dao.getVaultNoteById(id)?.toDecryptedDomain(key)
        }

    override suspend fun saveVaultNote(note: Note): Long =
        keyProvider.withUnlockedVaultKey { key ->
            val now = System.currentTimeMillis()
            val vaultNote = note.copy(isInVault = true)
            val encryptedEntity = vaultNote.toEncryptedEntity(
                key = key,
                lastModifiedDate = now
            )

            val savedId = if (vaultNote.id == 0L) {
                dao.insertNote(encryptedEntity)
            } else {
                dao.updateNote(encryptedEntity)
                vaultNote.id
            }

            // Encrypt in place any physical image files referenced by this
            // Vault note. The Vault key in scope is the same one just used to
            // write the row, so we never re-derive it here and never leak it
            // out of the unlocked block. The helper is best-effort over its
            // file inputs and idempotent against already-encrypted files; any
            // failure is captured as a non-sensitive warning and does not
            // revert the row write that already succeeded above.
            encryptVaultImagePaths(vaultNote.imagePaths, key)

            savedId
        } ?: throw VaultLockedException()

    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? =
        keyProvider.withUnlockedVaultKey { key ->
            when (val result = vaultImageFileStorage.decryptToByteArray(relativePath, key)) {
                is VaultImageFileDecryptionResult.Decrypted -> result.bytes
                VaultImageFileDecryptionResult.Missing -> null
            }
        }

    override suspend fun removeNoteFromVault(id: Long): Boolean =
        keyProvider.withUnlockedVaultKey { key ->
            val imagePaths = database.withTransaction {
                val source = dao.getVaultNoteById(id)
                    ?: return@withTransaction null
                val now = System.currentTimeMillis()
                val normalNote = source.toDecryptedDomain(key)
                    .copy(isInVault = false, isDeleted = false, deletedDate = null)

                dao.updateNote(normalNote.toPlainEntity(lastModifiedDate = now))
                reindexNormalTags(noteId = id, content = normalNote.content, now = now)
                normalNote.imagePaths
            } ?: return@withUnlockedVaultKey false

            withContext(NonCancellable) {
                restoreVaultImagesForNormalNote(imagePaths, key)
            }

            true
        } ?: throw VaultLockedException()

    override suspend fun moveVaultNoteToTrash(id: Long): Boolean =
        keyProvider.withUnlockedVaultKey {
            dao.moveVaultNoteToTrash(id = id, deletedDate = System.currentTimeMillis()) > 0
        } ?: throw VaultLockedException()

    override suspend fun rewrapAllVaultNotesWith(newKey: SecretKey) {
        keyProvider.withUnlockedVaultKey { currentKey ->
            database.withTransaction {
                val vaultEntities = dao.getAllVaultNotesOnce()
                vaultEntities.forEach { entity ->
                    val rewrapped = entity.copy(
                        title = rewrapField(entity.title, currentKey, newKey),
                        content = rewrapField(entity.content, currentKey, newKey),
                        imagePathsRaw = rewrapField(entity.imagePathsRaw, currentKey, newKey)
                    )
                    dao.updateNote(rewrapped)
                }
            }
        } ?: throw VaultLockedException()
    }

    /**
     * Hard-delete every Vault note row in a single transaction, then best-effort
     * delete internal image files associated with those rows. The operation
     * requires an unlocked Vault only to decrypt the stored image path list; it
     * never decrypts or logs title/content.
     */
    override suspend fun wipeAllVaultNotes(): Int =
        keyProvider.withUnlockedVaultKey { key ->
            val (removedRows, imagePaths) = database.withTransaction {
                val vaultEntities = dao.getAllVaultNotesForWipeOnce()
                val paths = vaultEntities.flatMap { entity ->
                    decryptImagePaths(entity.imagePathsRaw, key)
                }
                dao.deleteAllVaultNotes() to paths
            }

            if (removedRows > 0) {
                deleteVaultImages(imagePaths)
            }
            removedRows
        } ?: throw VaultLockedException()

    /**
     * Encrypt in place every image file referenced by the active Vault note
     * with [noteId]. The Vault must be unlocked: the key is used only to
     * decrypt the stored image-path list, and the boundary never touches the
     * note row, the note flow, or any non-Vault note. Already-encrypted files
     * are detected by [VaultImageFileStorage] and skipped. Missing files do
     * not abort the operation: they are logged in a non-sensitive event and
     * the boundary still reports [VaultNoteImageEncryptionResult.Success] for
     * the note, because there is nothing left to protect on disk for that
     * path.
     */
    override suspend fun encryptImagesForVaultNote(
        noteId: Long
    ): VaultNoteImageEncryptionResult =
        keyProvider.withUnlockedVaultKey { key ->
            val entity = dao.getVaultNoteById(noteId)
                ?: return@withUnlockedVaultKey VaultNoteImageEncryptionResult.NoteNotFound

            val paths = decryptImagePaths(entity.imagePathsRaw, key)
            if (paths.isEmpty()) {
                return@withUnlockedVaultKey VaultNoteImageEncryptionResult.NoImages
            }

            encryptVaultImagePaths(paths, key)
            VaultNoteImageEncryptionResult.Success
        } ?: throw VaultLockedException()

    /**
     * Encrypt in place every non-blank, distinct image path under the given
     * Vault key. The helper is shared by save, move and the explicit
     * [encryptImagesForVaultNote] boundary so the file-level behavior
     * (idempotent, missing-tolerant, non-sensitive logging) stays identical.
     * Move uses [failOnError] so unexpected filesystem failures abort before
     * the normal note row is replaced by encrypted Vault fields.
     */
    private suspend fun encryptVaultImagePaths(
        paths: List<String>,
        key: SecretKey,
        failOnError: Boolean = false
    ) {
        paths.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { relativePath ->
                encryptVaultImageFile(relativePath, key, failOnError)
            }
    }

    private suspend fun encryptVaultImageFile(
        relativePath: String,
        key: SecretKey,
        failOnError: Boolean
    ): VaultImageFileEncryptionResult? {
        val result = runCatching {
            val encryptionResult = vaultImageFileStorage.encryptInPlace(relativePath, key)
            when (encryptionResult) {
                VaultImageFileEncryptionResult.Encrypted,
                VaultImageFileEncryptionResult.AlreadyEncrypted -> Unit
                VaultImageFileEncryptionResult.Missing ->
                    NexNoteDebugLog.repositoryWarning(event = "vaultImageEncryptMissing") {
                        "result=missing"
                    }
            }
            encryptionResult
        }.onFailure { error ->
            NexNoteDebugLog.repositoryWarning(event = "vaultImageEncryptFailed") {
                "error=${error::class.java.simpleName}"
            }
        }
        return if (failOnError) result.getOrThrow() else result.getOrNull()
    }

    private suspend fun encryptVaultImagePathsForMove(
        paths: List<String>,
        key: SecretKey
    ): List<String> {
        val encryptedPaths = mutableListOf<String>()
        try {
            paths.asSequence()
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { relativePath ->
                    val result = encryptVaultImageFile(
                        relativePath = relativePath,
                        key = key,
                        failOnError = true
                    )
                    if (result == VaultImageFileEncryptionResult.Encrypted) {
                        encryptedPaths += relativePath
                    }
                }
        } catch (error: Exception) {
            rollbackMoveEncryptedVaultImages(encryptedPaths, key)
            throw error
        }
        return encryptedPaths
    }

    private suspend fun rollbackMoveEncryptedVaultImages(
        paths: List<String>,
        key: SecretKey
    ) {
        paths.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { relativePath ->
                runCatching {
                    when (vaultImageFileStorage.decryptInPlace(relativePath, key)) {
                        VaultImageFileRestoreResult.Restored,
                        VaultImageFileRestoreResult.AlreadyPlaintext -> Unit
                        VaultImageFileRestoreResult.Missing ->
                            NexNoteDebugLog.repositoryWarning(event = "vaultImageMoveRollbackMissing") {
                                "result=missing"
                            }
                    }
                }.onFailure { error ->
                    NexNoteDebugLog.repositoryWarning(event = "vaultImageMoveRollbackFailed") {
                        "error=${error::class.java.simpleName}"
                    }
                }
            }
    }

    private suspend fun restoreVaultImagesForNormalNote(
        paths: List<String>,
        key: SecretKey
    ) {
        paths.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { relativePath ->
                runCatching {
                    when (vaultImageFileStorage.decryptInPlace(relativePath, key)) {
                        VaultImageFileRestoreResult.Restored,
                        VaultImageFileRestoreResult.AlreadyPlaintext -> Unit
                        VaultImageFileRestoreResult.Missing ->
                            NexNoteDebugLog.repositoryWarning(event = "vaultImageRemoveMissing") {
                                "result=missing"
                            }
                    }
                }.onFailure { error ->
                    NexNoteDebugLog.repositoryWarning(event = "vaultImageRemoveRestoreFailed") {
                        "error=${error::class.java.simpleName}"
                    }
                }
            }
    }

    private fun rewrapField(
        ciphertext: String,
        currentKey: SecretKey,
        newKey: SecretKey
    ): String {
        val plain = fieldCipher.decryptToString(ciphertext, currentKey)
        return fieldCipher.encryptToString(plain, newKey)
    }

    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult =
        keyProvider.withUnlockedVaultKey { key ->
            var encryptedImagePaths: List<String> = emptyList()
            try {
                database.withTransaction {
                    val source = dao.getNoteById(id)
                        ?.takeUnless { it.isDeleted }
                        ?: return@withTransaction MoveNoteToVaultResult.NotFound

                    val now = System.currentTimeMillis()
                    val normalNote = source.toPlainDomain()

                    encryptedImagePaths = encryptVaultImagePathsForMove(
                        paths = normalNote.imagePaths,
                        key = key
                    )

                    val encryptedEntity = normalNote
                        .copy(isInVault = true, isDeleted = false, deletedDate = null)
                        .toEncryptedEntity(key = key, lastModifiedDate = now)

                    dao.updateNote(encryptedEntity)
                    tagDao.deleteAllCrossRefsForNote(id)
                    tagDao.pruneOrphanTags()
                    MoveNoteToVaultResult.Success
                }
            } catch (error: Exception) {
                if (encryptedImagePaths.isNotEmpty()) {
                    withContext(NonCancellable) {
                        rollbackMoveEncryptedVaultImages(encryptedImagePaths, key)
                    }
                }
                throw error
            }
        } ?: throw VaultLockedException()

    private fun NoteEntity.toPlainDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        isMarkdown = isMarkdown,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate,
        timezone = timezone,
        isDeleted = isDeleted,
        deletedDate = deletedDate,
        isInVault = isInVault,
        isPinned = isPinned,
        imagePaths = parseImagePaths(imagePathsRaw),
        backgroundColor = backgroundColor,
        isPreviewMode = isPreviewMode
    )

    private fun NoteEntity.toDecryptedDomain(key: SecretKey): Note = Note(
        id = id,
        title = fieldCipher.decryptToString(title, key),
        content = fieldCipher.decryptToString(content, key),
        isMarkdown = isMarkdown,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate,
        timezone = timezone,
        isDeleted = isDeleted,
        deletedDate = deletedDate,
        isInVault = true,
        isPinned = isPinned,
        imagePaths = parseImagePaths(fieldCipher.decryptToString(imagePathsRaw, key)),
        backgroundColor = backgroundColor,
        isPreviewMode = isPreviewMode
    )

    private fun Note.toEncryptedEntity(
        key: SecretKey,
        lastModifiedDate: Long
    ): NoteEntity = NoteEntity(
        id = id,
        title = fieldCipher.encryptToString(title, key),
        content = fieldCipher.encryptToString(content, key),
        isMarkdown = isMarkdown,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate,
        timezone = timezone,
        isDeleted = isDeleted,
        deletedDate = deletedDate,
        isInVault = true,
        isPinned = isPinned,
        imagePathsRaw = fieldCipher.encryptToString(
            imagePaths.filter { it.isNotBlank() }.joinToString("\n"),
            key
        ),
        backgroundColor = backgroundColor,
        isPreviewMode = isPreviewMode
    )

    private fun parseImagePaths(raw: String): List<String> =
        raw.split('\n').filter { it.isNotBlank() }

    private fun decryptImagePaths(ciphertext: String, key: SecretKey): List<String> {
        if (ciphertext.isBlank()) return emptyList()
        return parseImagePaths(fieldCipher.decryptToString(ciphertext, key))
    }

    private suspend fun deleteVaultImages(imagePaths: List<String>) {
        imagePaths
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { relativePath ->
                runCatching {
                    val deleted = imageStorage.deleteImage(relativePath)
                    if (!deleted) {
                        NexNoteDebugLog.repositoryWarning(event = "vaultImageCleanupFailed") {
                            "result=false"
                        }
                    }
                }
                    .onFailure { error ->
                        NexNoteDebugLog.repositoryWarning(event = "vaultImageCleanupFailed") {
                            "error=${error::class.java.simpleName}"
                        }
                    }
            }
    }

    private suspend fun reindexNormalTags(noteId: Long, content: String, now: Long) {
        tagDao.deleteAllCrossRefsForNote(noteId)
        val tags = TagParser.extractTags(content)
        tags.forEach { tagName ->
            tagDao.insertTag(TagEntity(name = tagName, createdDate = now, lastUpdatedDate = now))
            tagDao.insertCrossRef(NoteTagCrossRef(noteId = noteId, tagName = tagName))
            tagDao.touchTag(tagName, now)
        }
        tagDao.pruneOrphanTags()
    }

    private fun Note.toPlainEntity(lastModifiedDate: Long): NoteEntity = NoteEntity(
        id = id,
        title = title,
        content = content,
        isMarkdown = isMarkdown,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate,
        timezone = timezone,
        isDeleted = isDeleted,
        deletedDate = deletedDate,
        isInVault = false,
        isPinned = isPinned,
        imagePathsRaw = imagePaths.filter { it.isNotBlank() }.joinToString("\n"),
        backgroundColor = backgroundColor,
        isPreviewMode = isPreviewMode
    )
}
