package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow

/**
 * Exposes the list of Vault notes from [VaultNoteRepository].
 *
 * The repository contract already guarantees that this flow only emits decrypted
 * note content when the Vault key is unlocked in memory; when the Vault is locked
 * the flow emits an empty list. This use case therefore does not need to inspect
 * the Vault state itself and never holds plaintext from a locked Vault.
 */
class ObserveVaultNotesUseCase(
    private val repository: VaultNoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.vaultNotes
    }
}
