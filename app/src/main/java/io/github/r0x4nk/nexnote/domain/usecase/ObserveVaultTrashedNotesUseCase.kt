package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow

/**
 * Exposes soft-deleted Vault notes from [VaultNoteRepository].
 *
 * The repository emits decrypted content only while the Vault key is unlocked
 * in memory. A locked Vault yields an empty list, so callers must still combine
 * this with Vault state before showing any protected trash surface.
 */
class ObserveVaultTrashedNotesUseCase(
    private val repository: VaultNoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.vaultTrashedNotes
    }
}
