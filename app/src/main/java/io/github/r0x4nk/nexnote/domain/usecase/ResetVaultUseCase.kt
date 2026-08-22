package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

/** Exposes the repository's authenticated, destructive Vault reset operation. */
class ResetVaultUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(): ResetVaultResult {
        return repository.resetVault()
    }
}
