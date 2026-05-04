package com.example.nexnote.di

import com.example.nexnote.domain.repository.IUserPreferencesRepository
import com.example.nexnote.domain.repository.NoteImageStorage
import com.example.nexnote.domain.repository.NoteRepository
import com.example.nexnote.domain.repository.TagRepository
import com.example.nexnote.domain.repository.TemplateRepository
import com.example.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import com.example.nexnote.domain.usecase.DeleteNoteImageUseCase
import com.example.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import com.example.nexnote.domain.usecase.DeleteTagUseCase
import com.example.nexnote.domain.usecase.DeleteTemplateUseCase
import com.example.nexnote.domain.usecase.EmptyTrashUseCase
import com.example.nexnote.domain.usecase.GetNoteByIdUseCase
import com.example.nexnote.domain.usecase.GetNoteImageFileUseCase
import com.example.nexnote.domain.usecase.GetTemplateByIdUseCase
import com.example.nexnote.domain.usecase.IndexNoteTagsUseCase
import com.example.nexnote.domain.usecase.MoveNoteToTrashUseCase
import com.example.nexnote.domain.usecase.ObserveAccentColorUseCase
import com.example.nexnote.domain.usecase.ObserveAllNotesSortedAscUseCase
import com.example.nexnote.domain.usecase.ObserveAllNotesUseCase
import com.example.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import com.example.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import com.example.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import com.example.nexnote.domain.usecase.ObserveFontScaleUseCase
import com.example.nexnote.domain.usecase.ObserveLeftHandedUseCase
import com.example.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import com.example.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import com.example.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import com.example.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import com.example.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import com.example.nexnote.domain.usecase.ObserveTemplatesUseCase
import com.example.nexnote.domain.usecase.ObserveThemeModeUseCase
import com.example.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import com.example.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import com.example.nexnote.domain.usecase.SaveNoteUseCase
import com.example.nexnote.domain.usecase.SaveTemplateUseCase
import com.example.nexnote.domain.usecase.SearchNotesScoredUseCase
import com.example.nexnote.domain.usecase.SearchTagsUseCase
import com.example.nexnote.domain.usecase.SetAccentColorUseCase
import com.example.nexnote.domain.usecase.SetFontScaleUseCase
import com.example.nexnote.domain.usecase.SetLeftHandedUseCase
import com.example.nexnote.domain.usecase.SetNoteCardStyleUseCase
import com.example.nexnote.domain.usecase.SetNotePreviewModeUseCase
import com.example.nexnote.domain.usecase.SetThemeModeUseCase
import com.example.nexnote.domain.usecase.SetTimezoneIdUseCase
import com.example.nexnote.domain.usecase.ToggleNotePinUseCase

internal class AppUseCases(
    noteRepository: NoteRepository,
    tagRepository: TagRepository,
    templateRepository: TemplateRepository,
    preferencesRepository: IUserPreferencesRepository,
    imageStorage: NoteImageStorage
) {
    val notes = NoteUseCases(noteRepository)
    val tags = TagUseCases(tagRepository)
    val templates = TemplateUseCases(templateRepository)
    val preferences = PreferencesUseCases(preferencesRepository)
    val images = ImageUseCases(imageStorage)
}

internal class NoteUseCases internal constructor(
    noteRepository: NoteRepository
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
    val setThemeMode = SetThemeModeUseCase(preferencesRepository)
    val setFontScale = SetFontScaleUseCase(preferencesRepository)
    val setTimezoneId = SetTimezoneIdUseCase(preferencesRepository)
    val setLeftHanded = SetLeftHandedUseCase(preferencesRepository)
    val setAccentColor = SetAccentColorUseCase(preferencesRepository)
    val setNoteCardStyle = SetNoteCardStyleUseCase(preferencesRepository)
}

internal class ImageUseCases internal constructor(
    imageStorage: NoteImageStorage
) {
    val copyNoteImageToInternal = CopyNoteImageToInternalUseCase(imageStorage)
    val deleteNoteImage = DeleteNoteImageUseCase(imageStorage)
    val getNoteImageFile = GetNoteImageFileUseCase(imageStorage)
}
