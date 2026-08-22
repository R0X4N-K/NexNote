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
 * The two Home indexes cover newest and oldest ordering while preserving the
 * pinned-first group. The creation-date index supports Agenda range queries.
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isDeleted", "isInVault", "isPinned", "lastModifiedDate"]),
        Index(
            value = ["isDeleted", "isInVault", "isPinned", "lastModifiedDate"],
            orders = [Index.Order.ASC, Index.Order.ASC, Index.Order.DESC, Index.Order.ASC],
            name = "index_notes_active_pinned_modified_asc"
        ),
        Index(value = ["isDeleted", "isInVault", "creationDate"])
    ]
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
