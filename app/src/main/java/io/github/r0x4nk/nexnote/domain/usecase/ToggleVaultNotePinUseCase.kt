package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

class ToggleVaultNotePinUseCase(
    private val repository: VaultNoteRepository
) {
    suspend operator fun invoke(note: Note): Boolean {
        if (!note.isInVault || note.isDeleted || note.id <= 0L) return false

        val savedId = repository.saveVaultNote(
            note.copy(isPinned = !note.isPinned)
        )
        return savedId == note.id
    }
}
