package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

/**
 * Updates the creation date of an unlocked Vault note.
 *
 * The note is reloaded through the encrypted repository and re-saved, so the
 * manifest is re-encrypted by the data layer. Trashed Vault notes are rejected.
 *
 * @return `true` when the note was updated, `false` when it is missing or
 * trashed. The data layer may still throw [io.github.r0x4nk.nexnote.domain.repository.VaultLockedException]
 * when the Vault is locked; callers must map that to a generic failure.
 */
class UpdateVaultNoteCreationDateUseCase(
    private val repository: VaultNoteRepository
) {
    suspend operator fun invoke(noteId: Long, creationDate: Long): Boolean {
        val note = repository.getVaultNoteById(noteId) ?: return false
        if (!note.isInVault || note.isDeleted) return false
        repository.saveVaultNote(note.copy(creationDate = creationDate))
        return true
    }
}
