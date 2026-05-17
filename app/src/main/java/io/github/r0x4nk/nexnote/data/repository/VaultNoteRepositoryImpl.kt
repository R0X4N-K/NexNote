package io.github.r0x4nk.nexnote.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.TagDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
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

internal class VaultNoteRepositoryImpl(
    private val database: RoomDatabase,
    private val dao: NoteDao,
    private val tagDao: TagDao,
    private val keyProvider: VaultUnlockedKeyProvider,
    private val imageStorage: NoteImageStorage,
    private val fieldCipher: VaultFieldCipher = VaultFieldCipher()
) : VaultNoteRepository, VaultNoteRewrapper, VaultNoteWiper {

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

            if (vaultNote.id == 0L) {
                dao.insertNote(encryptedEntity)
            } else {
                dao.updateNote(encryptedEntity)
                vaultNote.id
            }
        } ?: throw VaultLockedException()

    override suspend fun removeNoteFromVault(id: Long): Boolean =
        keyProvider.withUnlockedVaultKey { key ->
            database.withTransaction {
                val source = dao.getVaultNoteById(id)
                    ?: return@withTransaction false
                val now = System.currentTimeMillis()
                val normalNote = source.toDecryptedDomain(key)
                    .copy(isInVault = false, isDeleted = false, deletedDate = null)

                dao.updateNote(normalNote.toPlainEntity(lastModifiedDate = now))
                reindexNormalTags(noteId = id, content = normalNote.content, now = now)
                true
            }
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
            database.withTransaction {
                val source = dao.getNoteById(id)
                    ?.takeUnless { it.isDeleted }
                    ?: return@withTransaction MoveNoteToVaultResult.NotFound

                if (source.hasPlainImagePaths()) {
                    return@withTransaction MoveNoteToVaultResult.ContainsImages
                }

                val now = System.currentTimeMillis()
                val encryptedEntity = source.toPlainDomain()
                    .copy(isInVault = true, isDeleted = false, deletedDate = null)
                    .toEncryptedEntity(key = key, lastModifiedDate = now)

                dao.updateNote(encryptedEntity)
                tagDao.deleteAllCrossRefsForNote(id)
                tagDao.pruneOrphanTags()
                MoveNoteToVaultResult.Success
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

    private fun NoteEntity.hasPlainImagePaths(): Boolean =
        parseImagePaths(imagePathsRaw).isNotEmpty()

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
