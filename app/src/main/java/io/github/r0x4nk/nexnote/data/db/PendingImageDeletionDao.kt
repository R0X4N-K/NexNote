package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.r0x4nk.nexnote.data.db.entity.PendingImageDeletionEntity

@Dao
interface PendingImageDeletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entries: List<PendingImageDeletionEntity>)

    @Query("SELECT relativePath FROM pending_image_deletions WHERE relativePath > :after ORDER BY relativePath LIMIT :limit")
    suspend fun nextBatch(after: String, limit: Int): List<String>

    @Query("DELETE FROM pending_image_deletions WHERE relativePath = :path")
    suspend fun remove(path: String)
}
