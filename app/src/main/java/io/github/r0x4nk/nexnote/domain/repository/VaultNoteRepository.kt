package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface VaultNoteRepository {
    val vaultNotes: Flow<List<Note>>
    val vaultTrashedNotes: Flow<List<Note>>
    val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>>

    /**
     * Hashtags extracted in memory from the content of *active* Vault notes
     * whenever the Vault is unlocked, matching the normal tag index semantics.
     * The list is aggregated by tag name (lowercased, without leading '#'),
     * with [Tag.noteCount] counting Vault notes that contain the tag and
     * [Tag.createdDate]/[Tag.lastUpdatedDate] derived from the notes' own
     * non-encrypted timestamps.
     *
     * Security contract:
     * - Emits an empty list whenever the Vault is locked or the key is not in
     *   memory.
     * - Never writes to the normal `tags` or `note_tag_cross_refs` tables, and
     *   never indexes Vault content into any persistent search structure.
     * - Tag names are derived only from already-decrypted in-memory note
     *   content; deleted Vault notes (trash) are excluded.
     * - The implementation must not log tag names, titles, contents, image
     *   paths, PIN, key bytes or any other Vault-sensitive material.
     */
    val vaultTags: Flow<List<Tag>>

    suspend fun getVaultNoteById(id: Long): Note?
    suspend fun saveVaultNote(note: Note): Long
    suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult
    suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult
    suspend fun removeNoteFromVault(id: Long): Boolean
    suspend fun moveVaultNoteToTrash(id: Long): Boolean
    suspend fun restoreVaultNoteFromTrash(id: Long): Boolean
    suspend fun deleteVaultNotePermanently(id: Long): Boolean

    /**
     * Decrypt the physical image file at [relativePath] and return the raw
     * plaintext bytes, or `null` when the Vault is locked or the file does not
     * exist on disk.
     *
     * The returned [ByteArray] is a fresh copy; the caller is responsible for
     * zeroing it after use if the content is sensitive. The implementation
     * never logs, persists or exposes the decrypted bytes, the path, the key
     * or any other Vault material.
     */
    suspend fun decryptVaultImageBytes(relativePath: String): ByteArray?
}

sealed interface MoveNoteToVaultResult {
    data object Success : MoveNoteToVaultResult
    data object NotFound : MoveNoteToVaultResult
}

sealed interface DuplicateVaultNoteResult {
    data class Success(val noteId: Long) : DuplicateVaultNoteResult
    data object NotFound : DuplicateVaultNoteResult
    data object Failed : DuplicateVaultNoteResult
}

class VaultLockedException : IllegalStateException("Vault is locked.")
