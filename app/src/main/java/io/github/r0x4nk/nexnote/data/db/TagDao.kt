package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the tag system.
 *
 * Role: data layer — Room DAO for `tags` and `note_tag_cross_ref` tables.
 *
 * Usage counts are computed dynamically via joins with `note_tag_cross_ref` and
 * `notes` (filtered to active normal notes: isDeleted = 0 and isInVault = 0).
 * Storing a cached count in the entity would require transactional updates on
 * every note save, trash, restore, and permanent delete — error-prone and prone
 * to drift. Dynamic computation is preferred here because tag queries are
 * infrequent relative to note saves.
 *
 * All queries returning [Flow] are observed reactively by the UI layer. Room
 * re-emits automatically when any of the involved tables change.
 */
@Dao
interface TagDao {

    // ── Tag queries ───────────────────────────────────────────────────────────

    /** Tags sorted by active-note usage, most used first. */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT n.id) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName
        INNER JOIN notes n ON r.noteId = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY t.name
        ORDER BY noteCount DESC, t.name ASC
    """)
    fun getAllTagsByUsageDesc(): Flow<List<TagWithCount>>

    /** Tags sorted by active-note usage, least used first. */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT n.id) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName
        INNER JOIN notes n ON r.noteId = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY t.name
        ORDER BY noteCount ASC, t.name ASC
    """)
    fun getAllTagsByUsageAsc(): Flow<List<TagWithCount>>

    /** Tags sorted by last-updated date, most recently used first. */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT n.id) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName
        INNER JOIN notes n ON r.noteId = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY t.name
        ORDER BY t.lastUpdatedDate DESC
    """)
    fun getAllTagsByDateDesc(): Flow<List<TagWithCount>>

    /** Tags sorted by last-updated date, oldest first. */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT n.id) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName
        INNER JOIN notes n ON r.noteId = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY t.name
        ORDER BY t.lastUpdatedDate ASC
    """)
    fun getAllTagsByDateAsc(): Flow<List<TagWithCount>>

    /** Tags whose name contains [query], sorted by usage descending. */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT n.id) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName
        INNER JOIN notes n ON r.noteId = n.id
        WHERE t.name LIKE '%' || :query || '%'
          AND n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY t.name
        ORDER BY noteCount DESC, t.name ASC
    """)
    fun searchTagsByName(query: String): Flow<List<TagWithCount>>

    /** Tags associated with a specific note (includes tags in trashed notes). */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT CASE WHEN n.isDeleted = 0 AND n.isInVault = 0 THEN r2.noteId END) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName AND r.noteId = :noteId
        INNER JOIN notes target ON target.id = r.noteId AND target.isInVault = 0
        LEFT JOIN note_tag_cross_ref r2 ON t.name = r2.tagName
        LEFT JOIN notes n ON r2.noteId = n.id
        GROUP BY t.name
        ORDER BY t.name ASC
    """)
    fun getTagsForNote(noteId: Long): Flow<List<TagWithCount>>

    /** Top [limit] most-used tags; used by the AutoScrollingTagRow on Home/Agenda. */
    @Query("""
        SELECT t.name, t.createdDate, t.lastUpdatedDate,
               COUNT(DISTINCT n.id) AS noteCount
        FROM tags t
        INNER JOIN note_tag_cross_ref r ON t.name = r.tagName
        INNER JOIN notes n ON r.noteId = n.id
        WHERE n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY t.name
        ORDER BY noteCount DESC, t.name ASC
        LIMIT :limit
    """)
    fun getMostUsedTags(limit: Int): Flow<List<TagWithCount>>

    /**
     * Note IDs whose tags contain ALL entries in [tagNames] (intersection).
     * [tagCount] must equal [tagNames].size — Room does not support computed
     * parameters, so the caller is responsible for passing the correct value.
     * This query is only called when [tagNames] is non-empty.
     */
    @Query("""
        SELECT r.noteId
        FROM note_tag_cross_ref r
        INNER JOIN notes n ON r.noteId = n.id
        WHERE r.tagName IN (:tagNames)
          AND n.isDeleted = 0
          AND n.isInVault = 0
        GROUP BY r.noteId
        HAVING COUNT(DISTINCT r.tagName) = :tagCount
    """)
    fun getNoteIdsWithAllTags(tagNames: List<String>, tagCount: Int): Flow<List<Long>>

    /** All cross-refs for a note; used during re-indexing to diff old vs. new tags. */
    @Query("SELECT * FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun getCrossRefsForNote(noteId: Long): List<NoteTagCrossRef>

    // ── Tag mutations ─────────────────────────────────────────────────────────

    /** Inserts a new tag; silently ignored when the name already exists (IGNORE). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    /**
     * Updates [lastUpdatedDate] for a tag that already exists in the database.
     * Called when a note containing the tag is saved, so "date" sort reflects
     * the most recent time a tag was actively used.
     */
    @Query("UPDATE tags SET lastUpdatedDate = :updatedDate WHERE name = :name")
    suspend fun touchTag(name: String, updatedDate: Long)

    /** Removes the tag entity row for [name]. */
    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun deleteTagByName(name: String)

    /**
     * Removes tag entities that have no remaining cross-refs.
     * Called after re-indexing removes some cross-refs to clean up orphan tags.
     */
    @Query("""
        DELETE FROM tags
        WHERE name NOT IN (SELECT DISTINCT tagName FROM note_tag_cross_ref)
    """)
    suspend fun pruneOrphanTags()

    // ── Cross-ref mutations ───────────────────────────────────────────────────

    /** Inserts a note–tag association; silently ignored when it already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    /** Removes the cross-ref for a specific (note, tag) pair. */
    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagName = :tagName")
    suspend fun deleteCrossRef(noteId: Long, tagName: String)

    /** Removes all cross-refs for a note; used when a note is permanently deleted. */
    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllCrossRefsForNote(noteId: Long)

    /** Removes cross-refs for a tag from normal notes; Vault refs are preserved. */
    @Query("""
        DELETE FROM note_tag_cross_ref
        WHERE tagName = :tagName
          AND noteId IN (SELECT id FROM notes WHERE isInVault = 0)
    """)
    suspend fun deleteNonVaultCrossRefsForTag(tagName: String)
}

/**
 * Room query result POJO that combines [TagEntity] fields with the computed
 * [noteCount]. Room maps columns to fields by name, so the column alias in each
 * SQL query (`COUNT(...) AS noteCount`) must match the field name here exactly.
 */
data class TagWithCount(
    val name: String,
    val createdDate: Long,
    val lastUpdatedDate: Long,
    val noteCount: Int
)
