package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Join table linking notes to their tags.
 *
 * Role: data layer — Room entity for the `note_tag_cross_ref` table.
 *
 * Design decisions:
 * - CASCADE DELETE on the note side ensures cross-refs are cleaned up automatically
 *   when a note is permanently deleted from the database.
 * - Soft-deleted (trashed) notes retain their cross-refs so that tag associations
 *   survive a restore. The usage-count queries JOIN with `notes WHERE isDeleted = 0`
 *   to exclude trashed notes from the displayed count.
 * - No foreign key on [tagName]: the tag entity is managed separately by
 *   [TagRepository.deleteTag], which removes all cross-refs in the same operation.
 *   Avoiding the FK here also prevents cascades during tag cleanup from interfering
 *   with note content updates.
 */
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity        = NoteEntity::class,
            parentColumns = ["id"],
            childColumns  = ["noteId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tagName"])]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagName: String   // Lowercase, without the '#' prefix
)
