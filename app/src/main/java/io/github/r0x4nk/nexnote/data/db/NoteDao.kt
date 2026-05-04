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
        ORDER BY isPinned DESC, lastModifiedDate DESC
    """
    )
    fun getAllNotes(): Flow<List<NoteEntity>>

    /** Active notes: pinned first, then by last-modified date oldest to newest. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
        ORDER BY isPinned DESC, lastModifiedDate ASC
    """
    )
    fun getAllNotesSortedAsc(): Flow<List<NoteEntity>>

    /** Trashed notes ordered by deletion date, newest first. */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 1
        ORDER BY deletedDate DESC
    """
    )
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    /**
     * Active note-link targets without note bodies. The editor needs these for
     * autocomplete and validation, and loading full content here hurts startup.
     */
    @Query(
        """
        SELECT id, title FROM notes
        WHERE isDeleted = 0
        ORDER BY title COLLATE NOCASE ASC, id ASC
    """
    )
    fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>>

    /** Single note lookup by id. Returns null when the note does not exist. */
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    /**
     * Basic LIKE search on title and content.
     * Advanced ranking (scoring) is applied in SearchUtils after this query returns.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
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
    @Query("SELECT creationDate FROM notes WHERE isDeleted = 0 ORDER BY creationDate ASC")
    fun getAllCreationDates(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, deletedDate = :deletedDate WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedDate: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedDate = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM notes WHERE id = :id AND isDeleted = 1")
    suspend fun deleteNotePermanently(id: Long): Int

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash(): Int

    @Query("SELECT imagePathsRaw FROM notes WHERE isDeleted = 1 AND imagePathsRaw != ''")
    suspend fun getDeletedImagePathsRaw(): List<String>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE notes SET isPreviewMode = :isPreviewMode WHERE id = :id")
    suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean)

}
