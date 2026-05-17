package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Query

/**
 * Minimal note-content patch API used by cross-aggregate maintenance jobs.
 *
 * Keeping this DAO narrow avoids exposing the full note persistence surface to
 * repositories that only need to patch text as part of index maintenance.
 */
@Dao
interface NoteContentPatchDao {

    @Query(
        """
        SELECT n.id, n.content
        FROM notes n
        INNER JOIN note_tag_cross_ref r ON n.id = r.noteId
        WHERE r.tagName = :tagName
          AND n.isInVault = 0
        """
    )
    suspend fun getPatchesForTag(tagName: String): List<NoteContentPatch>

    @Query(
        """
        UPDATE notes
        SET content = :content, lastModifiedDate = :lastModifiedDate
        WHERE id = :id
          AND isInVault = 0
    """
    )
    suspend fun updateContent(id: Long, content: String, lastModifiedDate: Long)
}

data class NoteContentPatch(
    val id: Long,
    val content: String
)
