package io.github.r0x4nk.nexnote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    /** All templates: predefined first (alphabetical), then custom (alphabetical). */
    @Query(
        """
        SELECT * FROM templates
        ORDER BY isPredefined DESC, name ASC
    """
    )
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): TemplateEntity?

    /** Counts predefined templates, used to seed the built-in set on first launch. */
    @Query("SELECT COUNT(*) FROM templates WHERE isPredefined = 1")
    suspend fun countPredefinedTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    /** Deletes a template row selected by the user. */
    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
}
