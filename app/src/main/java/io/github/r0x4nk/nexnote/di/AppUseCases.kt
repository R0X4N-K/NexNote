package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.NoteAttachmentStorage
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteStatisticsRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

internal class AppUseCases(
    noteRepository: NoteRepository,
    tagRepository: TagRepository,
    templateRepository: TemplateRepository,
    preferencesRepository: IUserPreferencesRepository,
    statisticsRepository: NoteStatisticsRepository,
    imageStorage: NoteImageStorage,
    attachmentStorage: NoteAttachmentStorage,
    vaultRepository: VaultRepository,
    vaultAndroidCredentialRepository: VaultAndroidCredentialRepository,
    vaultNoteRepository: VaultNoteRepository
) {
    val notes = NoteUseCases(noteRepository, tagRepository, imageStorage)
    val statistics = StatisticsUseCases(statisticsRepository)
    val tags = TagUseCases(tagRepository)
    val templates = TemplateUseCases(templateRepository)
    val preferences = PreferencesUseCases(preferencesRepository)
    val images = ImageUseCases(imageStorage)
    val attachments = AttachmentUseCases(attachmentStorage)
    val storedNotes = StoredNotesUseCases(noteRepository, vaultNoteRepository)
    val vault = VaultUseCases(
        vaultRepository = vaultRepository,
        vaultAndroidCredentialRepository = vaultAndroidCredentialRepository,
        vaultNoteRepository = vaultNoteRepository
    )
}
