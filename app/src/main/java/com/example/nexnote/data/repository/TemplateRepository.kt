package com.example.nexnote.data.repository

import com.example.nexnote.data.db.TemplateDao
import com.example.nexnote.data.db.entity.TemplateEntity
import com.example.nexnote.domain.model.PredefinedTemplates
import com.example.nexnote.domain.model.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TemplateRepository(
    private val dao: TemplateDao
) : com.example.nexnote.domain.repository.TemplateRepository {

    /** Flow of all templates: predefined first, then custom templates alphabetically. */
    override val allTemplates: Flow<List<Template>> =
        dao.getAllTemplates().map { list -> list.map { it.toDomain() } }

    override suspend fun getTemplateById(id: Long): Template? =
        dao.getTemplateById(id)?.toDomain()

    /**
     * Saves a custom template. Inserts when id == 0; otherwise updates.
     * Returns the persisted id.
     */
    override suspend fun saveTemplate(template: Template): Long {
        require(!template.isPredefined) { "Predefined templates cannot be modified." }
        return if (template.id == 0L) {
            dao.insertTemplate(template.toEntity())
        } else {
            dao.updateTemplate(template.toEntity())
            template.id
        }
    }

    /**
     * Deletes a custom template. Predefined templates cannot be deleted;
     * attempting to do so throws IllegalArgumentException.
     */
    override suspend fun deleteTemplate(template: Template) {
        require(!template.isPredefined) { "Predefined templates cannot be deleted." }
        dao.deleteTemplate(template.toEntity())
    }

    /**
     * Inserts predefined templates if they are not already in the database.
     * Called from NexNoteApp.onCreate() on a background coroutine.
     */
    suspend fun initializePredefinedTemplates() {
        if (dao.countPredefinedTemplates() == 0) {
            PredefinedTemplates.all.forEach { template ->
                dao.insertTemplate(template.toEntity())
            }
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun TemplateEntity.toDomain(): Template = Template(
        id = id,
        name = name,
        content = content,
        isMarkdown = isMarkdown,
        category = category,
        isPredefined = isPredefined,
        iconName = iconName
    )

    private fun Template.toEntity(): TemplateEntity = TemplateEntity(
        id = id,
        name = name,
        content = content,
        isMarkdown = isMarkdown,
        category = category,
        isPredefined = isPredefined,
        iconName = iconName
    )
}
