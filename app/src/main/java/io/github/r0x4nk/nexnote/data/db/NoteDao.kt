package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /** Active notes: pinned first, then by last-modified date newest to oldest. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        ORDER BY isPinned DESC, lastModifiedDate DESC
    """
    )
    fun getAllNotes(): Flow<List<NoteEntity>>

    /** Active notes: pinned first, then by last-modified date oldest to newest. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        ORDER BY isPinned DESC, lastModifiedDate ASC
    """
    )
    fun getAllNotesSortedAsc(): Flow<List<NoteEntity>>

    /** Trashed notes ordered by deletion date, newest first. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 1
          AND isInVault = 0
        ORDER BY deletedDate DESC
    """
    )
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    /** Trashed Vault notes. Callers must decrypt fields only after the Vault is unlocked. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 1
          AND isInVault = 1
        ORDER BY deletedDate DESC
    """
    )
    fun getDeletedVaultNotes(): Flow<List<NoteEntity>>

    /**
     * Active note-link targets without note bodies. The editor needs these for
     * autocomplete and validation, and loading full content here hurts startup.
     */
    @Query(
        """
        SELECT id, title FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        ORDER BY title COLLATE NOCASE ASC, id ASC
    """
    )
    fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>>

    /**
     * Active Vault note-link targets without note bodies. Titles are encrypted
     * at rest; callers must decrypt them only after the Vault is unlocked.
     */
    @Query(
        """
        SELECT id, title FROM notes
        WHERE isDeleted = 0
          AND isInVault = 1
        ORDER BY id ASC
    """
    )
    fun getVaultNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>>

    /** Single normal-note lookup by id. Vault notes require an explicit unlocked Vault path. */
    @Query("SELECT * FROM notes WHERE id = :id AND isInVault = 0 LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    /** Active Vault notes. Callers must decrypt fields only after the Vault is unlocked. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 1
        ORDER BY isPinned DESC, lastModifiedDate DESC
    """
    )
    fun getAllVaultNotes(): Flow<List<NoteEntity>>

    /** Single active Vault-note lookup by id. Callers must require an unlocked Vault key. */
    @Query(
        """
        SELECT * FROM notes
        WHERE id = :id
          AND isDeleted = 0
          AND isInVault = 1
        LIMIT 1
    """
    )
    suspend fun getVaultNoteById(id: Long): NoteEntity?

    /**
     * Active Vault notes as a one-shot list. Intended for transactional bulk
     * operations such as re-encrypting all Vault entries with a new key.
     * Callers must require an unlocked Vault key before decrypting fields.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 1
    """
    )
    suspend fun getAllVaultNotesOnce(): List<NoteEntity>

    /**
     * Every Vault note row, including soft-deleted rows. Intended only for the
     * reset/wipe boundary so associated resources can be inspected before the
     * rows are hard-deleted.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE isInVault = 1
    """
    )
    suspend fun getAllVaultNotesForWipeOnce(): List<NoteEntity>

    /**
     * One soft-deleted Vault note row for single permanent-delete cleanup.
     * Callers must require an unlocked Vault before decrypting image paths.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE id = :id
          AND isInVault = 1
          AND isDeleted = 1
        LIMIT 1
    """
    )
    suspend fun getDeletedVaultNoteById(id: Long): NoteEntity?

    /**
     * Hard-delete every Vault note row, whether currently active or already in
     * the trash. Intended exclusively for the Vault reset boundary: the
     * encrypted payloads are unrecoverable without the previous key, so wiping
     * them is the only coherent option when the Vault is reset. Normal notes
     * (active or trashed) are left untouched.
     */
    @Query("DELETE FROM notes WHERE isInVault = 1")
    suspend fun deleteAllVaultNotes(): Int

    /**
     * Basic LIKE search on title and content.
     * Advanced ranking (scoring) is applied in SearchUtils after this query returns.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, lastModifiedDate DESC
    """
    )
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    /**
     * Active notes whose creationDate falls in [startMs, endMs).
     * Used by AgendaViewModel and ExportViewModel.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
          AND creationDate >= :startMs
          AND creationDate < :endMs
        ORDER BY creationDate ASC
    """
    )
    fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<NoteEntity>>

    /**
     * All creationDate timestamps (ms) for active notes.
     * The repository maps these to start-of-day values for the agenda calendar.
     */
    @Query(
        """
        SELECT creationDate FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        ORDER BY creationDate ASC
    """
    )
    fun getAllCreationDates(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query(
        """
        UPDATE notes
        SET isDeleted = 1, deletedDate = :deletedDate
        WHERE id = :id
          AND isInVault = 0
    """
    )
    suspend fun moveToTrash(id: Long, deletedDate: Long)

    @Query(
        """
        UPDATE notes
        SET isDeleted = 1, deletedDate = :deletedDate
        WHERE id = :id
          AND isInVault = 1
          AND isDeleted = 0
    """
    )
    suspend fun moveVaultNoteToTrash(id: Long, deletedDate: Long): Int

    @Query(
        """
        UPDATE notes
        SET isDeleted = 0, deletedDate = NULL
        WHERE id = :id
          AND isInVault = 0
    """
    )
    suspend fun restoreFromTrash(id: Long)

    @Query(
        """
        UPDATE notes
        SET isDeleted = 0, deletedDate = NULL
        WHERE id = :id
          AND isInVault = 1
          AND isDeleted = 1
    """
    )
    suspend fun restoreVaultNoteFromTrash(id: Long): Int

    @Query("DELETE FROM notes WHERE id = :id AND isDeleted = 1 AND isInVault = 0")
    suspend fun deleteNotePermanently(id: Long): Int

    @Query("DELETE FROM notes WHERE id = :id AND isDeleted = 1 AND isInVault = 1")
    suspend fun deleteVaultNotePermanently(id: Long): Int

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND isInVault = 0")
    suspend fun emptyTrash(): Int

    @Query(
        """
        SELECT imagePathsRaw FROM notes
        WHERE isDeleted = 1
          AND isInVault = 0
          AND imagePathsRaw != ''
    """
    )
    suspend fun getDeletedImagePathsRaw(): List<String>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id AND isInVault = 0")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query(
        """
        UPDATE notes
        SET isPreviewMode = :isPreviewMode
        WHERE id = :id
          AND isInVault = 0
    """
    )
    suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean)

}
