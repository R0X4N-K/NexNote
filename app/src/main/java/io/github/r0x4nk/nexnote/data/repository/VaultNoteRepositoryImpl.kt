package io.github.r0x4nk.nexnote.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.NoteStatisticsDao
import io.github.r0x4nk.nexnote.data.db.TagDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.data.local.VaultImageFileDecryptionResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileEncryptionResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileRestoreResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileRewrapBackup
import io.github.r0x4nk.nexnote.data.local.VaultImageFileStorage
import io.github.r0x4nk.nexnote.data.security.VaultDecryptionException
import io.github.r0x4nk.nexnote.data.security.VaultFieldCipher
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.VaultLockedException
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.usecase.NoteStatisticsTextAnalyzer
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.TagParser
import io.github.r0x4nk.nexnote.util.rewriteMappedPaths
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.crypto.SecretKey

/**
 * Internal data-layer boundary that re-encrypts every Vault note with a
 * freshly derived key. Exposed only inside the data package so callers that
 * coordinate PIN changes can swap the key without leaking [SecretKey] into the
 * domain contracts. Implementations must require the Vault to be currently
 * unlocked: rewrap reads ciphertext using the existing key, decrypts it in
 * memory, and writes back ciphertext under the new key.
 */
internal interface VaultNoteRewrapper {
    /**
     * Re-encrypt all Vault notes and images from [currentKey] to [newKey]. The
     * returned transaction retains encrypted rollback material until the
     * caller either commits after persisting the new PIN configuration or
     * rolls back while the old configuration is still authoritative.
     */
    suspend fun rewrapAllVaultNotesWith(
        currentKey: SecretKey,
        newKey: SecretKey
    ): VaultNoteRewrapTransaction
}

internal interface VaultNoteRewrapTransaction {
    suspend fun commit()
    suspend fun rollback()
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
 * The contract remains inside the data package so save, move, and editor flows
 * can use it without exposing the Vault [SecretKey] through domain APIs.
 * Implementations must require the Vault to be currently unlocked.
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
        VaultImageFileStorage(imageStorage = imageStorage),
    private val statisticsDao: NoteStatisticsDao? = null
) : VaultNoteRepository, VaultNoteRewrapper, VaultNoteWiper, VaultNoteImageEncryptor {

    override val allVaultNoteCount: Flow<Int> =
        dao.observeAllVaultNoteCount().distinctUntilChanged()

    override val vaultNotes: Flow<List<Note>> =
        combine(
            dao.getAllVaultNotes().distinctUntilChanged(),
            keyProvider.unlockedVaultKey
        ) { entities, key ->
            if (key == null) {
                emptyList()
            } else {
                entities.mapNotNull { entity ->
                    entity.toDecryptedDomainOrNull(key, event = "vaultNoteDecryptionSkipped")
                }
            }
        }.distinctUntilChanged()

    override val vaultTrashedNotes: Flow<List<Note>> =
        combine(
            dao.getDeletedVaultNotes().distinctUntilChanged(),
            keyProvider.unlockedVaultKey
        ) { entities, key ->
            if (key == null) {
                emptyList()
            } else {
                entities.mapNotNull { entity ->
                    entity.toDecryptedDomainOrNull(key, event = "vaultTrashDecryptionSkipped")
                }
            }
        }.distinctUntilChanged()

    override val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>> =
        combine(
            dao.getVaultNoteLinkCandidates().distinctUntilChanged(),
            keyProvider.unlockedVaultKey
        ) { projections, key ->
            if (key == null) {
                emptyList()
            } else {
                projections.mapNotNull { projection ->
                    projection.toVaultNoteLinkCandidateOrNull(
                        key = key,
                        event = "vaultLinkCandidateDecryptionSkipped"
                    )
                }
            }
        }.distinctUntilChanged()

    override suspend fun getVaultNoteById(id: Long): Note? =
        keyProvider.withUnlockedVaultKey { key ->
            dao.getVaultNoteById(id)
                ?.toDecryptedDomainOrNull(key, event = "vaultNoteLookupDecryptionSkipped")
        }

    override suspend fun saveVaultNote(note: Note): Long =
        keyProvider.withUnlockedVaultKey { key ->
            val now = System.currentTimeMillis()
            val vaultNote = note.copy(isInVault = true)
            val encryptedEntity = vaultNote.toEncryptedEntity(
                key = key,
                lastModifiedDate = now
            )

            database.withTransaction {
                val savedId = if (vaultNote.id == 0L) {
                    dao.insertNote(encryptedEntity)
                } else {
                    dao.updateNote(encryptedEntity)
                    vaultNote.id
                }
                statisticsDao?.delete(savedId)

                // A Vault row must never commit while it points at a plaintext
                // image file. Missing files remain non-fatal because there is
                // no on-disk payload to protect, but unexpected encryption
                // failures abort the Room transaction so the old encrypted row
                // stays authoritative.
                encryptVaultImagePaths(
                    paths = vaultNote.imagePaths,
                    key = key,
                    failOnError = true
                )

                savedId
            }
        } ?: throw VaultLockedException()

    override suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult =
        keyProvider.withUnlockedVaultKey { key ->
            val copiedImagePaths = mutableListOf<String>()
            try {
                database.withTransaction {
                    val source = dao.getVaultNoteById(id)?.toDecryptedDomain(key)
                        ?: return@withTransaction DuplicateVaultNoteResult.NotFound

                    val now = System.currentTimeMillis()
                    val draft = source.copy(
                        id = 0L,
                        isInVault = true,
                        isDeleted = false,
                        deletedDate = null
                    )
                    val duplicateId = dao.insertNote(
                        draft.toEncryptedEntity(key = key, lastModifiedDate = now)
                    )
                    val imagePathMap = copyVaultImagePaths(
                        newNoteId = duplicateId,
                        sourcePaths = source.imagePaths,
                        key = key,
                        copiedPaths = copiedImagePaths
                    )
                    encryptVaultImagePaths(
                        paths = imagePathMap.values.toList(),
                        key = key,
                        failOnError = true
                    )

                    val duplicate = draft.copy(
                        id = duplicateId,
                        content = draft.content.rewriteMappedPaths(imagePathMap),
                        imagePaths = draft.imagePaths.map { path -> imagePathMap[path] ?: path }
                    )
                    dao.updateNote(duplicate.toEncryptedEntity(key = key, lastModifiedDate = now))
                    DuplicateVaultNoteResult.Success(duplicateId)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (copiedImagePaths.isNotEmpty()) {
                    withContext(NonCancellable) {
                        deleteVaultImages(copiedImagePaths)
                    }
                }
                DuplicateVaultNoteResult.Failed
            }
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
                statisticsDao?.upsert(
                    NoteStatisticsTextAnalyzer.analyze(
                        noteId = id,
                        content = normalNote.content,
                        creationDate = normalNote.creationDate,
                        lastModifiedDate = now
                    ).toEntity()
                )
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

    override suspend fun restoreVaultNoteFromTrash(id: Long): Boolean =
        keyProvider.withUnlockedVaultKey {
            dao.restoreVaultNoteFromTrash(id) > 0
        } ?: throw VaultLockedException()

    override suspend fun deleteVaultNotePermanently(id: Long): Boolean =
        keyProvider.withUnlockedVaultKey { key ->
            val (deletedRows, imagePaths) = database.withTransaction {
                val entity = dao.getDeletedVaultNoteById(id)
                    ?: return@withTransaction 0 to emptyList()
                // A corrupted trashed row must still be permanently deletable;
                // when its `imagePathsRaw` cannot be decrypted we skip file
                // cleanup for it instead of failing the delete.
                val paths = decryptImagePathsOrEmpty(
                    ciphertext = entity.imagePathsRaw,
                    key = key,
                    noteId = id,
                    event = "vaultPermanentDeleteImagePathsDecryptionSkipped"
                )
                dao.deleteVaultNotePermanently(id) to paths
            }

            if (deletedRows > 0) {
                withContext(NonCancellable) {
                    deleteVaultImages(imagePaths)
                }
            }
            deletedRows > 0
        } ?: throw VaultLockedException()

    override suspend fun deleteAllVaultNotesPermanently(): Int = wipeAllVaultNotes()

    override suspend fun rewrapAllVaultNotesWith(
        currentKey: SecretKey,
        newKey: SecretKey
    ): VaultNoteRewrapTransaction {
        val callerContext = currentCoroutineContext()
        var originalEntities = emptyList<NoteEntity>()
        val imageBackups = mutableListOf<VaultImageFileRewrapBackup>()

        try {
            database.withTransaction {
                // The all-rows query includes active and soft-deleted Vault
                // entries; despite its historical name it is the shared
                // complete-Vault snapshot boundary for destructive/rekey work.
                originalEntities = dao.getAllVaultNotesForWipeOnce()
                val rewrappedEntities = originalEntities.map { entity ->
                    entity.copy(
                        title = rewrapField(entity.title, currentKey, newKey),
                        content = rewrapField(entity.content, currentKey, newKey),
                        imagePathsRaw = rewrapField(entity.imagePathsRaw, currentKey, newKey)
                    )
                }
                val imagePaths = originalEntities
                    .flatMap { entity -> decryptImagePaths(entity.imagePathsRaw, currentKey) }
                    .filter { it.isNotBlank() }
                    .distinct()

                imagePaths.forEach { relativePath ->
                    callerContext.ensureActive()
                    // A file replacement and registration of its encrypted
                    // backup are one non-cancellable step. Cancellation is
                    // observed immediately afterwards and rolls back the full
                    // Room/filesystem operation. Check the captured caller
                    // context rather than Room's transaction context so a
                    // cancellation requested during this step cannot race the
                    // following row updates.
                    withContext(NonCancellable) {
                        vaultImageFileStorage.rewrapInPlace(
                            relativePath = relativePath,
                            currentKey = currentKey,
                            newKey = newKey,
                            onBackupCreated = imageBackups::add
                        )
                    }
                    callerContext.ensureActive()
                }

                rewrappedEntities.forEach { entity -> dao.updateNote(entity) }
            }
        } catch (error: Throwable) {
            val rollbackFailure = withContext(NonCancellable) {
                restoreImageBackups(vaultImageFileStorage, imageBackups)
                    ?: deleteImageBackups(vaultImageFileStorage, imageBackups)
            }
            rollbackFailure?.let(error::addSuppressed)
            throw error
        }

        return RepositoryVaultNoteRewrapTransaction(
            database = database,
            dao = dao,
            originalEntities = originalEntities,
            imageBackups = imageBackups,
            vaultImageFileStorage = vaultImageFileStorage
        )
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
                // Reset must always be able to drop encrypted rows, including a
                // corrupted one whose `imagePathsRaw` can no longer be decrypted.
                // For such a row we cannot recover its physical image paths, so we
                // skip best-effort file cleanup for it and still delete the row.
                val paths = vaultEntities.flatMap { entity ->
                    decryptImagePathsOrEmpty(
                        ciphertext = entity.imagePathsRaw,
                        key = key,
                        noteId = entity.id,
                        event = "vaultWipeImagePathsDecryptionSkipped"
                    )
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
     * Save and move use [failOnError] so unexpected filesystem failures abort
     * before a row can point at an unprotected Vault image file.
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
        val result = runCatchingPreservingCancellation {
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
                runCatchingPreservingCancellation {
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
                runCatchingPreservingCancellation {
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
                    statisticsDao?.delete(id)
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

    private fun NoteLinkCandidateProjection.toVaultNoteLinkCandidate(
        key: SecretKey
    ): NoteLinkCandidate = NoteLinkCandidate(
        id = id,
        title = fieldCipher.decryptToString(title, key)
    )

    private fun NoteEntity.toDecryptedDomainOrNull(
        key: SecretKey,
        event: String
    ): Note? =
        try {
            toDecryptedDomain(key)
        } catch (error: VaultDecryptionException) {
            NexNoteDebugLog.repositoryWarning(event = event) {
                "noteId=$id error=${error::class.java.simpleName}"
            }
            null
        }

    private fun NoteLinkCandidateProjection.toVaultNoteLinkCandidateOrNull(
        key: SecretKey,
        event: String
    ): NoteLinkCandidate? =
        try {
            toVaultNoteLinkCandidate(key)
        } catch (error: VaultDecryptionException) {
            NexNoteDebugLog.repositoryWarning(event = event) {
                "noteId=$id error=${error::class.java.simpleName}"
            }
            null
        }

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

    /**
     * Decrypt the stored image-path list for a single Vault row, tolerating a
     * corrupted or otherwise undecryptable [ciphertext]. Used only by the
     * destructive flows (reset wipe, permanent delete) where the row must be
     * removed even when its encrypted payload can no longer be read. When the
     * paths cannot be recovered we cannot know which physical files belong to
     * the row, so the caller skips best-effort file cleanup for it; any
     * leftover files stay Vault-encrypted on disk and never expose plaintext.
     * Only a non-sensitive event/id/error-class is logged.
     */
    private fun decryptImagePathsOrEmpty(
        ciphertext: String,
        key: SecretKey,
        noteId: Long,
        event: String
    ): List<String> =
        try {
            decryptImagePaths(ciphertext, key)
        } catch (error: VaultDecryptionException) {
            NexNoteDebugLog.repositoryWarning(event = event) {
                "noteId=$noteId error=${error::class.java.simpleName}"
            }
            emptyList()
        }

    private suspend fun copyVaultImagePaths(
        newNoteId: Long,
        sourcePaths: List<String>,
        key: SecretKey,
        copiedPaths: MutableList<String>
    ): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        sourcePaths
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { sourcePath ->
                val bytes = when (val decrypted = vaultImageFileStorage.decryptToByteArray(sourcePath, key)) {
                    is VaultImageFileDecryptionResult.Decrypted -> decrypted.bytes
                    VaultImageFileDecryptionResult.Missing ->
                        throw IOException("Vault image file is missing.")
                }
                val duplicatePath = try {
                    imageStorage.copyImageToInternal(newNoteId) {
                        ByteArrayInputStream(bytes)
                    }
                } finally {
                    bytes.fill(0)
                }
                copiedPaths += duplicatePath
                result[sourcePath] = duplicatePath
            }
        return result
    }

    private suspend fun deleteVaultImages(imagePaths: List<String>) {
        imagePaths
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { relativePath ->
                runCatchingPreservingCancellation {
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

private class RepositoryVaultNoteRewrapTransaction(
    private val database: RoomDatabase,
    private val dao: NoteDao,
    private val originalEntities: List<NoteEntity>,
    private val imageBackups: List<VaultImageFileRewrapBackup>,
    private val vaultImageFileStorage: VaultImageFileStorage
) : VaultNoteRewrapTransaction {
    private var completed = false

    override suspend fun commit() {
        if (completed) return
        val cleanupFailure = deleteImageBackups(vaultImageFileStorage, imageBackups)
        completed = true
        cleanupFailure?.let { throw it }
    }

    override suspend fun rollback() {
        if (completed) return

        var failure: Throwable? = null
        try {
            database.withTransaction {
                originalEntities.forEach { entity -> dao.updateNote(entity) }
            }
        } catch (error: Throwable) {
            failure = error
        }

        val imageFailure = restoreImageBackups(vaultImageFileStorage, imageBackups)
        if (failure == null) {
            failure = imageFailure
        } else if (imageFailure != null) {
            failure.addSuppressed(imageFailure)
        }

        if (failure == null) {
            failure = deleteImageBackups(vaultImageFileStorage, imageBackups)
        }
        completed = failure == null
        failure?.let { throw it }
    }
}

private suspend fun restoreImageBackups(
    storage: VaultImageFileStorage,
    backups: List<VaultImageFileRewrapBackup>
): Throwable? {
    var failure: Throwable? = null
    backups.asReversed().forEach { backup ->
        try {
            storage.rollbackRewrap(backup)
        } catch (error: Throwable) {
            if (failure == null) {
                failure = error
            } else {
                failure?.addSuppressed(error)
            }
        }
    }
    return failure
}

private suspend fun deleteImageBackups(
    storage: VaultImageFileStorage,
    backups: List<VaultImageFileRewrapBackup>
): Throwable? {
    var failure: Throwable? = null
    backups.forEach { backup ->
        try {
            storage.commitRewrap(backup)
        } catch (error: Throwable) {
            if (failure == null) {
                failure = error
            } else {
                failure?.addSuppressed(error)
            }
        }
    }
    return failure
}
