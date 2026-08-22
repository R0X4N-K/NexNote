package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Persisted text analysis used to build statistics without loading note bodies. */
@Entity(
    tableName = "note_statistics_index",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteStatisticsIndexEntity(
    @PrimaryKey val noteId: Long,
    val creationDate: Long,
    val sourceLastModifiedDate: Long,
    val characterCount: Int,
    val wordCount: Int,
    val tagNamesRaw: String,
    val formatVersion: Int
)
