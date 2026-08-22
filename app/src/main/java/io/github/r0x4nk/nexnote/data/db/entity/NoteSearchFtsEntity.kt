package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/** Full-text search projection for active notes outside the Vault. */
@Fts4(
    contentEntity = NoteEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=0"],
    prefix = [2, 3]
)
@Entity(tableName = "notes_fts")
data class NoteSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val title: String,
    val content: String
)
