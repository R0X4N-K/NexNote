package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllNormalNoteCountUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<Int> = repository.allNormalNoteCount
}

class ObserveAllVaultNoteCountUseCase(
    private val repository: VaultNoteRepository
) {
    operator fun invoke(): Flow<Int> = repository.allVaultNoteCount
}

data class DeleteAllStoredNotesResult(
    val normalNotesDeleted: Int,
    val vaultNotesDeleted: Int
)

/** Deletes note rows only; templates, preferences and Vault configuration remain intact. */
class DeleteAllStoredNotesUseCase(
    private val noteRepository: NoteRepository,
    private val vaultNoteRepository: VaultNoteRepository
) {
    suspend operator fun invoke(includeVaultNotes: Boolean): DeleteAllStoredNotesResult {
        val normalNotesDeleted = noteRepository.deleteAllNormalNotesPermanently()
        val vaultNotesDeleted = if (includeVaultNotes) {
            vaultNoteRepository.deleteAllVaultNotesPermanently()
        } else {
            0
        }
        return DeleteAllStoredNotesResult(
            normalNotesDeleted = normalNotesDeleted,
            vaultNotesDeleted = vaultNotesDeleted
        )
    }
}
