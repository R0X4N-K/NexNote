package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.r0x4nk.nexnote.data.db.entity.NoteStatisticsIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteStatisticsDao {

    /** Valid index rows for active normal notes, without loading note bodies. */
    @Query(
        """
        SELECT i.* FROM note_statistics_index i
        INNER JOIN notes n ON n.id = i.noteId
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
          AND i.sourceLastModifiedDate = n.lastModifiedDate
          AND i.creationDate = n.creationDate
          AND i.formatVersion = :formatVersion
        ORDER BY i.noteId ASC
        """
    )
    fun observeIndexedNotes(formatVersion: Int): Flow<List<NoteStatisticsIndexEntity>>

    /** Counts valid rows and source rows without materialising note content. */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM notes
             WHERE isDeleted = 0 AND isInVault = 0) AS totalNotes,
            (SELECT COUNT(*) FROM note_statistics_index i
             INNER JOIN notes n ON n.id = i.noteId
             WHERE n.isDeleted = 0
               AND n.isInVault = 0
               AND i.sourceLastModifiedDate = n.lastModifiedDate
               AND i.creationDate = n.creationDate
               AND i.formatVersion = :formatVersion) AS indexedNotes
        """
    )
    fun observeIndexCounts(formatVersion: Int): Flow<NoteStatisticsIndexCounts>

    /** Returns a current progress snapshot for completion checks inside a drain pass. */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM notes
             WHERE isDeleted = 0 AND isInVault = 0) AS totalNotes,
            (SELECT COUNT(*) FROM note_statistics_index i
             INNER JOIN notes n ON n.id = i.noteId
             WHERE n.isDeleted = 0
               AND n.isInVault = 0
               AND i.sourceLastModifiedDate = n.lastModifiedDate
               AND i.creationDate = n.creationDate
               AND i.formatVersion = :formatVersion) AS indexedNotes
        """
    )
    suspend fun getIndexCounts(formatVersion: Int): NoteStatisticsIndexCounts

    /** Reads only one bounded batch of note bodies that needs analysis. */
    @Query(
        """
        SELECT n.id, n.content, n.creationDate, n.lastModifiedDate
        FROM notes n
        LEFT JOIN note_statistics_index i ON i.noteId = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
          AND n.id > :afterNoteId
          AND (
              i.noteId IS NULL
              OR i.sourceLastModifiedDate != n.lastModifiedDate
              OR i.creationDate != n.creationDate
              OR i.formatVersion != :formatVersion
          )
        ORDER BY n.id ASC
        LIMIT :limit
        """
    )
    suspend fun getNextIndexBatch(
        formatVersion: Int,
        afterNoteId: Long,
        limit: Int
    ): List<NoteStatisticsSource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entries: List<NoteStatisticsIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: NoteStatisticsIndexEntity)

    @Query("DELETE FROM note_statistics_index WHERE noteId = :noteId")
    suspend fun delete(noteId: Long)

    @Query("DELETE FROM note_statistics_index")
    suspend fun clear()
}

data class NoteStatisticsIndexCounts(
    val totalNotes: Int,
    val indexedNotes: Int
)

data class NoteStatisticsSource(
    val id: Long,
    val content: String,
    val creationDate: Long,
    val lastModifiedDate: Long
)
