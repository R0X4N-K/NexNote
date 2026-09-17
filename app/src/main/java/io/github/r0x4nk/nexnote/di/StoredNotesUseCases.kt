package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.usecase.DeleteAllStoredNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNormalNoteCountUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllVaultNoteCountUseCase

internal class StoredNotesUseCases internal constructor(
    noteRepository: NoteRepository,
    vaultNoteRepository: VaultNoteRepository
) {
    val observeAllNormalNoteCount = ObserveAllNormalNoteCountUseCase(noteRepository)
    val observeAllVaultNoteCount = ObserveAllVaultNoteCountUseCase(vaultNoteRepository)
    val deleteAll = DeleteAllStoredNotesUseCase(noteRepository, vaultNoteRepository)
}
