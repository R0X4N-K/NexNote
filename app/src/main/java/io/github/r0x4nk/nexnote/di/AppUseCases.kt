package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.usecase.ChangeVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ClearVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ConfigureVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DecryptVaultImageBytesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTagUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.EmptyTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteImageFileUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultAndroidCredentialAvailabilityUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveVaultNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesSortedAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveLeftHandedUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultRecentPreviewsProtectionUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RefreshVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ResetVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchNotesScoredUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetLeftHandedUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNotePreviewModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultRecentPreviewsProtectionUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithAndroidCredentialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithPinUseCase

internal class AppUseCases(
    noteRepository: NoteRepository,
    tagRepository: TagRepository,
    templateRepository: TemplateRepository,
    preferencesRepository: IUserPreferencesRepository,
    imageStorage: NoteImageStorage,
    vaultRepository: VaultRepository,
    vaultAndroidCredentialRepository: VaultAndroidCredentialRepository,
    vaultNoteRepository: VaultNoteRepository
) {
    val notes = NoteUseCases(noteRepository, tagRepository, imageStorage)
    val tags = TagUseCases(tagRepository)
    val templates = TemplateUseCases(templateRepository)
    val preferences = PreferencesUseCases(preferencesRepository)
    val images = ImageUseCases(imageStorage)
    val vault = VaultUseCases(
        vaultRepository = vaultRepository,
        vaultAndroidCredentialRepository = vaultAndroidCredentialRepository,
        vaultNoteRepository = vaultNoteRepository
    )
}

internal class NoteUseCases internal constructor(
    noteRepository: NoteRepository,
    tagRepository: TagRepository,
    imageStorage: NoteImageStorage
) {
    val getNoteById = GetNoteByIdUseCase(noteRepository)
    val searchNotesScored = SearchNotesScoredUseCase(noteRepository)
    val observeAllNotes = ObserveAllNotesUseCase(noteRepository)
    val observeAllNotesSortedAsc = ObserveAllNotesSortedAscUseCase(noteRepository)
    val observeNoteLinkCandidates = ObserveNoteLinkCandidatesUseCase(noteRepository)
    val observeDeletedNotes = ObserveDeletedNotesUseCase(noteRepository)
    val observeDistinctLocalDays = ObserveDistinctLocalDaysUseCase(noteRepository)
    val observeNotesByDateRange = ObserveNotesByDateRangeUseCase(noteRepository)
    val moveNoteToTrash = MoveNoteToTrashUseCase(noteRepository)
    val restoreNoteFromTrash = RestoreNoteFromTrashUseCase(noteRepository)
    val deleteNotePermanently = DeleteNotePermanentlyUseCase(noteRepository)
    val emptyTrash = EmptyTrashUseCase(noteRepository)
    val toggleNotePin = ToggleNotePinUseCase(noteRepository)
    val saveNote = SaveNoteUseCase(noteRepository)
    val duplicateNote = DuplicateNoteUseCase(noteRepository, tagRepository, imageStorage)
    val setNotePreviewMode = SetNotePreviewModeUseCase(noteRepository)
}

internal class TagUseCases internal constructor(
    tagRepository: TagRepository
) {
    val observeTagsByUsageDesc = ObserveTagsByUsageDescUseCase(tagRepository)
    val observeTagsByUsageAsc = ObserveTagsByUsageAscUseCase(tagRepository)
    val observeTagsByDateDesc = ObserveTagsByDateDescUseCase(tagRepository)
    val observeTagsByDateAsc = ObserveTagsByDateAscUseCase(tagRepository)
    val observeTagsForNote = ObserveTagsForNoteUseCase(tagRepository)
    val observeMostUsedTags = ObserveMostUsedTagsUseCase(tagRepository)
    val observeFilteredNoteIds = ObserveFilteredNoteIdsUseCase(tagRepository)
    val searchTags = SearchTagsUseCase(tagRepository)
    val indexNoteTags = IndexNoteTagsUseCase(tagRepository)
    val deleteTag = DeleteTagUseCase(tagRepository)
}

internal class TemplateUseCases internal constructor(
    templateRepository: TemplateRepository
) {
    val observeTemplates = ObserveTemplatesUseCase(templateRepository)
    val getTemplateById = GetTemplateByIdUseCase(templateRepository)
    val saveTemplate = SaveTemplateUseCase(templateRepository)
    val deleteTemplate = DeleteTemplateUseCase(templateRepository)
}

internal class PreferencesUseCases internal constructor(
    preferencesRepository: IUserPreferencesRepository
) {
    val observeThemeMode = ObserveThemeModeUseCase(preferencesRepository)
    val observeFontScale = ObserveFontScaleUseCase(preferencesRepository)
    val observeTimezoneId = ObserveTimezoneIdUseCase(preferencesRepository)
    val observeLeftHanded = ObserveLeftHandedUseCase(preferencesRepository)
    val observeAccentColor = ObserveAccentColorUseCase(preferencesRepository)
    val observeNoteCardStyle = ObserveNoteCardStyleUseCase(preferencesRepository)
    val observeVaultRecentPreviewsProtection =
        ObserveVaultRecentPreviewsProtectionUseCase(preferencesRepository)
    val observeVaultLockOnBackground =
        ObserveVaultLockOnBackgroundUseCase(preferencesRepository)
    val observeVaultAutoLockTimeout =
        ObserveVaultAutoLockTimeoutUseCase(preferencesRepository)
    val observeVaultAndroidCredentialUnlock =
        ObserveVaultAndroidCredentialUnlockUseCase(preferencesRepository)
    val setThemeMode = SetThemeModeUseCase(preferencesRepository)
    val setFontScale = SetFontScaleUseCase(preferencesRepository)
    val setTimezoneId = SetTimezoneIdUseCase(preferencesRepository)
    val setLeftHanded = SetLeftHandedUseCase(preferencesRepository)
    val setAccentColor = SetAccentColorUseCase(preferencesRepository)
    val setNoteCardStyle = SetNoteCardStyleUseCase(preferencesRepository)
    val setVaultRecentPreviewsProtection =
        SetVaultRecentPreviewsProtectionUseCase(preferencesRepository)
    val setVaultLockOnBackground =
        SetVaultLockOnBackgroundUseCase(preferencesRepository)
    val setVaultAutoLockTimeout =
        SetVaultAutoLockTimeoutUseCase(preferencesRepository)
    val setVaultAndroidCredentialUnlock =
        SetVaultAndroidCredentialUnlockUseCase(preferencesRepository)
}

internal class ImageUseCases internal constructor(
    imageStorage: NoteImageStorage
) {
    val copyNoteImageToInternal = CopyNoteImageToInternalUseCase(imageStorage)
    val deleteNoteImage = DeleteNoteImageUseCase(imageStorage)
    val getNoteImageFile = GetNoteImageFileUseCase(imageStorage)
}

internal class VaultUseCases internal constructor(
    vaultRepository: VaultRepository,
    vaultAndroidCredentialRepository: VaultAndroidCredentialRepository,
    vaultNoteRepository: VaultNoteRepository
) {
    val observeVaultState = ObserveVaultStateUseCase(vaultRepository)
    val configureVaultPin = ConfigureVaultPinUseCase(vaultRepository)
    val unlockVaultWithPin = UnlockVaultWithPinUseCase(vaultRepository)
    val unlockVaultWithAndroidCredential =
        UnlockVaultWithAndroidCredentialUseCase(vaultRepository)
    val observeVaultAndroidCredentialProtectedMaterial =
        ObserveVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    val refreshVaultAndroidCredentialProtectedMaterial =
        RefreshVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    val clearVaultAndroidCredentialProtectedMaterial =
        ClearVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    val changeVaultPin = ChangeVaultPinUseCase(vaultRepository)
    val resetVault = ResetVaultUseCase(vaultRepository)
    val getVaultAndroidCredentialAvailability =
        GetVaultAndroidCredentialAvailabilityUseCase(vaultAndroidCredentialRepository)
    val lockVault = LockVaultUseCase(vaultRepository)
    val decryptVaultImageBytes = DecryptVaultImageBytesUseCase(vaultNoteRepository)
    val moveNoteToVault = MoveNoteToVaultUseCase(vaultNoteRepository)
    val moveVaultNoteToTrash = MoveVaultNoteToTrashUseCase(vaultNoteRepository)
    val observeVaultNotes = ObserveVaultNotesUseCase(vaultNoteRepository)
    val getVaultNoteById = GetVaultNoteByIdUseCase(vaultNoteRepository)
    val saveVaultNote = SaveVaultNoteUseCase(vaultNoteRepository)
    val removeNoteFromVault = RemoveNoteFromVaultUseCase(vaultNoteRepository)
}
