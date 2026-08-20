package io.github.r0x4nk.nexnote.testing

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal object NoOpTagRepository : TagRepository {
    override fun getAllTagsByUsageDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByUsageAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun searchTags(query: String): Flow<List<Tag>> = flowOf(emptyList())
    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> = flowOf(emptyList())
    override fun getMostUsedTags(limit: Int): Flow<List<Tag>> = flowOf(emptyList())
    override fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>> = flowOf(emptySet())
    override suspend fun indexNoteTags(noteId: Long, content: String) = Unit
    override suspend fun deleteTag(tagName: String) = Unit
}

internal object NoOpTemplateRepository : TemplateRepository {
    override val allTemplates: Flow<List<Template>> = flowOf(emptyList())
    override suspend fun getTemplateById(id: Long): Template? = null
    override suspend fun saveTemplate(template: Template): Long = template.id
    override suspend fun deleteTemplate(template: Template) = Unit
}

internal object NoOpPreferencesRepository : IUserPreferencesRepository {
    override val themeMode = flowOf(ThemeMode.SYSTEM)
    override val fontScale = flowOf(FontScale.NORMAL)
    override val timezoneId = flowOf("UTC")
    override val isLeftHanded = flowOf(false)
    override val accentColor = flowOf(AccentColor.VIOLET)
    override val noteCardStyle = flowOf(NoteCardStyle.TITLE_AND_PREVIEW)
    override val protectVaultRecentPreviews = flowOf(true)
    override val lockVaultOnBackground = flowOf(true)
    override val vaultAutoLockTimeout = flowOf(VaultAutoLockTimeout.IMMEDIATELY)
    override val unlockVaultWithAndroidCredential = flowOf(false)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
    override suspend fun setFontScale(scale: FontScale) = Unit
    override suspend fun setTimezoneId(id: String) = Unit
    override suspend fun setLeftHanded(value: Boolean) = Unit
    override suspend fun setAccentColor(color: AccentColor) = Unit
    override suspend fun setNoteCardStyle(style: NoteCardStyle) = Unit
    override suspend fun setProtectVaultRecentPreviews(value: Boolean) = Unit
    override suspend fun setLockVaultOnBackground(value: Boolean) = Unit
    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) = Unit
    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) = Unit
}
