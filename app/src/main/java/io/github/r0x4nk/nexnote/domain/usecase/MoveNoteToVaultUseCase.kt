package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

class MoveNoteToVaultUseCase(
    private val repository: VaultNoteRepository
) {
    suspend operator fun invoke(noteId: Long): MoveNoteToVaultResult {
        return repository.moveNormalNoteToVault(noteId)
    }
}
