package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface VaultNoteRepository {
    val vaultNotes: Flow<List<Note>>

    suspend fun getVaultNoteById(id: Long): Note?
    suspend fun saveVaultNote(note: Note): Long
    suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult
    suspend fun removeNoteFromVault(id: Long): Boolean
}

sealed interface MoveNoteToVaultResult {
    data object Success : MoveNoteToVaultResult
    data object NotFound : MoveNoteToVaultResult
    data object ContainsImages : MoveNoteToVaultResult
}

class VaultLockedException : IllegalStateException("Vault is locked.")
