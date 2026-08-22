package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Query
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/** Bounded Home queries that avoid materialising the entire notes table. */
@Dao
interface HomeNoteDao {

    /** Active normal-note ids used by selection without loading note bodies. */
    @Query(
        """
        SELECT id FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        """
    )
    fun observeActiveNoteIds(): Flow<List<Long>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        ORDER BY isPinned DESC, lastModifiedDate DESC
        LIMIT :limit
        """
    )
    fun observeRecentNotes(limit: Int): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        ORDER BY isPinned DESC, lastModifiedDate ASC
        LIMIT :limit
        """
    )
    fun observeOldestModifiedNotes(limit: Int): Flow<List<NoteEntity>>

    /** Uses FTS4 to reduce text candidates before tag filtering and materialisation. */
    @Query(
        """
        SELECT n.* FROM notes n
        INNER JOIN notes_fts ON notes_fts.rowid = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
          AND notes_fts MATCH :matchQuery
          AND (
              :pinnedFilter = 0
              OR (:pinnedFilter = 1 AND n.isPinned = 1)
              OR (:pinnedFilter = 2 AND n.isPinned = 0)
          )
          AND (
              :tagCount = 0
              OR n.id IN (
                  SELECT r.noteId
                  FROM note_tag_cross_ref r
                  WHERE r.tagName IN (:tagNames)
                  GROUP BY r.noteId
                  HAVING COUNT(DISTINCT r.tagName) = :tagCount
              )
          )
        ORDER BY
          n.isPinned DESC,
          CASE WHEN :sortMode = 0 AND
              INSTR(LOWER(n.title), LOWER(:rawQuery)) = 1 THEN 1 ELSE 0 END DESC,
          CASE WHEN :sortMode = 0 THEN n.lastModifiedDate END DESC,
          CASE WHEN :sortMode = 1 THEN n.lastModifiedDate END DESC,
          CASE WHEN :sortMode = 2 THEN n.lastModifiedDate END ASC,
          CASE WHEN :sortMode = 3 THEN n.title END COLLATE NOCASE ASC,
          CASE WHEN :sortMode = 4 THEN n.title END COLLATE NOCASE DESC,
          n.id DESC
        LIMIT :limit
        """
    )
    fun observeSearchResults(
        matchQuery: String,
        rawQuery: String,
        sortMode: Int,
        pinnedFilter: Int,
        tagNames: List<String>,
        tagCount: Int,
        limit: Int
    ): Flow<List<NoteEntity>>

    /** FTS candidates for select-all; only ids cross the Room boundary. */
    @Query(
        """
        SELECT n.id FROM notes n
        INNER JOIN notes_fts ON notes_fts.rowid = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
          AND notes_fts MATCH :matchQuery
          AND (
              :pinnedFilter = 0
              OR (:pinnedFilter = 1 AND n.isPinned = 1)
              OR (:pinnedFilter = 2 AND n.isPinned = 0)
          )
          AND (
              :tagCount = 0
              OR n.id IN (
                  SELECT r.noteId
                  FROM note_tag_cross_ref r
                  WHERE r.tagName IN (:tagNames)
                  GROUP BY r.noteId
                  HAVING COUNT(DISTINCT r.tagName) = :tagCount
              )
          )
        """
    )
    fun observeSearchResultIds(
        matchQuery: String,
        pinnedFilter: Int,
        tagNames: List<String>,
        tagCount: Int
    ): Flow<List<Long>>

    /** Fallback for symbol-only searches and filtered lists without text tokens. */
    @Query(
        """
        SELECT n.* FROM notes n
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
          AND (
              :query = ''
              OR (:searchScope = 0 AND (
                  INSTR(LOWER(n.title), LOWER(:query)) > 0
                  OR INSTR(LOWER(n.content), LOWER(:query)) > 0
              ))
              OR (:searchScope = 1 AND INSTR(LOWER(n.title), LOWER(:query)) > 0)
              OR (:searchScope = 2 AND INSTR(LOWER(n.content), LOWER(:query)) > 0)
          )
          AND (
              :pinnedFilter = 0
              OR (:pinnedFilter = 1 AND n.isPinned = 1)
              OR (:pinnedFilter = 2 AND n.isPinned = 0)
          )
          AND (
              :tagCount = 0
              OR n.id IN (
                  SELECT r.noteId
                  FROM note_tag_cross_ref r
                  WHERE r.tagName IN (:tagNames)
                  GROUP BY r.noteId
                  HAVING COUNT(DISTINCT r.tagName) = :tagCount
              )
          )
        ORDER BY
          n.isPinned DESC,
          CASE WHEN :query != '' AND :sortMode = 0 THEN
              (CASE WHEN INSTR(LOWER(n.title), LOWER(:query)) = 1 THEN 5 ELSE 0 END) +
              ((LENGTH(LOWER(n.title)) - LENGTH(REPLACE(LOWER(n.title), LOWER(:query), ''))) /
                  LENGTH(:query)) * 3 +
              ((LENGTH(LOWER(n.content)) - LENGTH(REPLACE(LOWER(n.content), LOWER(:query), ''))) /
                  LENGTH(:query))
          ELSE 0 END DESC,
          CASE WHEN :query = '' AND :sortAscending = 1 THEN n.lastModifiedDate END ASC,
          CASE WHEN :query = '' AND :sortAscending = 0 THEN n.lastModifiedDate END DESC,
          CASE WHEN :query != '' AND :sortMode = 0 THEN n.lastModifiedDate END DESC,
          CASE WHEN :query != '' AND :sortMode = 1 THEN n.lastModifiedDate END DESC,
          CASE WHEN :query != '' AND :sortMode = 2 THEN n.lastModifiedDate END ASC,
          CASE WHEN :query != '' AND :sortMode = 3 THEN n.title END COLLATE NOCASE ASC,
          CASE WHEN :query != '' AND :sortMode = 4 THEN n.title END COLLATE NOCASE DESC,
          n.id DESC
        LIMIT :limit
        """
    )
    fun observeFilteredHomeNotes(
        query: String,
        sortAscending: Boolean,
        sortMode: Int,
        searchScope: Int,
        pinnedFilter: Int,
        tagNames: List<String>,
        tagCount: Int,
        limit: Int
    ): Flow<List<NoteEntity>>

    /** Literal and filter-only select-all query that avoids materialising content. */
    @Query(
        """
        SELECT n.id FROM notes n
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
          AND (
              :query = ''
              OR (:searchScope = 0 AND (
                  INSTR(LOWER(n.title), LOWER(:query)) > 0
                  OR INSTR(LOWER(n.content), LOWER(:query)) > 0
              ))
              OR (:searchScope = 1 AND INSTR(LOWER(n.title), LOWER(:query)) > 0)
              OR (:searchScope = 2 AND INSTR(LOWER(n.content), LOWER(:query)) > 0)
          )
          AND (
              :pinnedFilter = 0
              OR (:pinnedFilter = 1 AND n.isPinned = 1)
              OR (:pinnedFilter = 2 AND n.isPinned = 0)
          )
          AND (
              :tagCount = 0
              OR n.id IN (
                  SELECT r.noteId
                  FROM note_tag_cross_ref r
                  WHERE r.tagName IN (:tagNames)
                  GROUP BY r.noteId
                  HAVING COUNT(DISTINCT r.tagName) = :tagCount
              )
          )
        """
    )
    fun observeFilteredHomeNoteIds(
        query: String,
        searchScope: Int,
        pinnedFilter: Int,
        tagNames: List<String>,
        tagCount: Int
    ): Flow<List<Long>>

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE isDeleted = 0
          AND isInVault = 0
        """
    )
    fun observeActiveNoteCount(): Flow<Int>
}
