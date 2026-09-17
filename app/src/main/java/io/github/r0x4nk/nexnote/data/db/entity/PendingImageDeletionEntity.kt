package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** File cleanup survives process death after the owning notes have been removed. */
@Entity(tableName = "pending_image_deletions")
data class PendingImageDeletionEntity(@PrimaryKey val relativePath: String)
