package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

class DuplicateVaultNoteUseCase(
    private val repository: VaultNoteRepository
) {
    suspend operator fun invoke(noteId: Long): DuplicateVaultNoteResult {
        return repository.duplicateVaultNote(noteId)
    }
}
