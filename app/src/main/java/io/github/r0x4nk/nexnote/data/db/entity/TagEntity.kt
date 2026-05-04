package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent record for a tag extracted from note content.
 *
 * Role: data layer — Room entity for the `tags` table.
 *
 * [name] is the canonical (lowercased) tag identifier stored without the leading
 * '#'. Usage count is NOT stored here; it is computed dynamically via SQL JOIN
 * in [TagDao] to prevent denormalisation drift over time.
 *
 * [createdDate] is set on first insertion and never updated.
 * [lastUpdatedDate] is refreshed whenever any note containing this tag is saved,
 * so "date" sort reflects recency of use rather than creation order.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val createdDate: Long = System.currentTimeMillis(),
    val lastUpdatedDate: Long = System.currentTimeMillis()
)
