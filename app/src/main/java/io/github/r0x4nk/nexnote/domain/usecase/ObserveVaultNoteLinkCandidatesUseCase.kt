package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight Vault-scoped note-link targets for the editor.
 *
 * The repository emits decrypted titles only while the Vault key is unlocked in
 * memory and emits an empty list when locked, so this use case does not inspect
 * Vault state or hold sensitive content itself.
 */
class ObserveVaultNoteLinkCandidatesUseCase(
    private val repository: VaultNoteRepository
) {
    operator fun invoke(): Flow<List<NoteLinkCandidate>> {
        return repository.vaultNoteLinkCandidates
    }
}
