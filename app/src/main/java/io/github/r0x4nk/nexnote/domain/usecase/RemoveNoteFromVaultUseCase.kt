package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

class RemoveNoteFromVaultUseCase(
    private val repository: VaultNoteRepository
) {
    suspend operator fun invoke(noteId: Long): Boolean {
        return repository.removeNoteFromVault(noteId)
    }
}
