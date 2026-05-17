package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for notes.
 *
 * [imagePathsRaw]: newline-separated image paths relative to context.filesDir.
 *   Empty string means no images.
 * [deletedDate]: null when the note is not in the trash.
 * [creationDate]: user-editable (for the agenda calendar).
 * [lastModifiedDate]: managed exclusively by the app, not user-editable.
 *
 * The composite index on (isDeleted, isInVault, isPinned, lastModifiedDate) directly covers
 * the ORDER BY clause of getAllNotes() and its ascending variant, so those queries
 * avoid a full table scan even with thousands of notes.
 */
@Entity(
    tableName = "notes",
    indices = [Index(value = ["isDeleted", "isInVault", "isPinned", "lastModifiedDate"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val isMarkdown: Boolean = true,
    val creationDate: Long = System.currentTimeMillis(),
    val lastModifiedDate: Long = System.currentTimeMillis(),
    val timezone: String = java.util.TimeZone.getDefault().id,
    val isDeleted: Boolean = false,
    val deletedDate: Long? = null,
    // True when the note belongs to the encrypted Vault surface.
    // Added in database version 7; normal-note queries must exclude it.
    val isInVault: Boolean = false,
    val isPinned: Boolean = false,
    val imagePathsRaw: String = "",
    // Packed ARGB color (android.graphics.Color.toArgb()). NULL means "no custom color".
    // Added in database version 3; existing rows default to NULL via migration.
    val backgroundColor: Int? = null,
    // True when the note was last viewed in Markdown preview mode.
    // Added in database version 5; existing rows default to 0 (edit mode) via migration.
    val isPreviewMode: Boolean = false
)
