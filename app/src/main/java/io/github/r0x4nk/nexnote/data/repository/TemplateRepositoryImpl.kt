package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.data.db.TemplateDao
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import io.github.r0x4nk.nexnote.domain.model.PredefinedTemplates
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of the domain [TemplateRepository] contract.
 *
 * The `Impl` suffix keeps the data-layer implementation distinct from the
 * domain interface, matching the note and tag repositories.
 */
class TemplateRepositoryImpl(
    private val dao: TemplateDao
) : TemplateRepository {

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

    /** Deletes a template selected from the templates screen. */
    override suspend fun deleteTemplate(template: Template) {
        dao.deleteTemplate(template.toEntity())
    }

    /**
     * Inserts predefined templates if they are not already in the database.
     * Called once from NexNoteApp.onCreate() on a background coroutine.
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
