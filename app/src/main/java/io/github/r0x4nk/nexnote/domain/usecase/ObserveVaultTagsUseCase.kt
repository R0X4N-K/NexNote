package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow

/**
 * Exposes the in-memory hashtag aggregation derived from active Vault notes.
 *
 * The repository contract already guarantees that the underlying flow emits an
 * empty list whenever the Vault is locked, so this use case never observes
 * plaintext Vault content while the Vault key is not in memory. The use case
 * is intentionally a thin pass-through: tag aggregation lives in the data
 * layer next to the decryption boundary, and this wrapper exists so that
 * ViewModels can depend on a use case instead of a repository, matching the
 * pattern used by the other Vault observation use cases.
 *
 * Security contract:
 * - Never writes tags to the normal `tags`/`note_tag_cross_refs` tables.
 * - Never logs tag names, titles, contents, image paths, PIN or key bytes.
 * - Emits an empty list whenever the Vault is locked.
 */
class ObserveVaultTagsUseCase(
    private val repository: VaultNoteRepository
) {
    operator fun invoke(): Flow<List<Tag>> = repository.vaultTags
}
