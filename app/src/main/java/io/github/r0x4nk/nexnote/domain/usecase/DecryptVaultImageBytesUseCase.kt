package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository

/**
 * Domain use case that decrypts a Vault image file and returns the raw
 * plaintext bytes, or `null` when the Vault is locked or the file does not
 * exist on disk.
 *
 * This is a pass-through to [VaultNoteRepository.decryptVaultImageBytes].
 * The use case never logs, persists or exposes the decrypted bytes, the
 * relative path, the Vault key or any other sensitive material.
 */
class DecryptVaultImageBytesUseCase(
    private val repository: VaultNoteRepository
) {
    /**
     * @return decrypted image bytes, or `null` when the Vault is locked or
     *         the file is missing.
     */
    suspend operator fun invoke(relativePath: String): ByteArray? {
        return repository.decryptVaultImageBytes(relativePath)
    }
}
