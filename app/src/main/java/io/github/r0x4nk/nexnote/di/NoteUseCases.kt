package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.usecase.BuildNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.EmptyTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveActiveNoteCountUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesSortedAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveHomeNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveHomeNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchNotesScoredUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNotePreviewModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UpdateNoteCreationDateUseCase

internal class NoteUseCases internal constructor(
    noteRepository: NoteRepository,
    tagRepository: TagRepository,
    imageStorage: NoteImageStorage
) {
    val buildNoteStatistics = BuildNoteStatisticsUseCase()
    val getNoteById = GetNoteByIdUseCase(noteRepository)
    val searchNotesScored = SearchNotesScoredUseCase(noteRepository)
    val observeAllNotes = ObserveAllNotesUseCase(noteRepository)
    val observeHomeNotes = ObserveHomeNotesUseCase(noteRepository)
    val observeHomeNoteIds = ObserveHomeNoteIdsUseCase(noteRepository)
    val observeActiveNoteCount = ObserveActiveNoteCountUseCase(noteRepository)
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
    val updateNoteCreationDate = UpdateNoteCreationDateUseCase(noteRepository)
}
