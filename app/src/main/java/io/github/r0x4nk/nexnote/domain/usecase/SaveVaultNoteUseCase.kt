package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

class SaveVaultNoteUseCase(
    private val repository: VaultNoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        return repository.saveVaultNote(note.copy(isInVault = true))
    }
}
