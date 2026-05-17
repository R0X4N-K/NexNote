package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

class UnlockVaultWithAndroidCredentialUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(): UnlockVaultWithAndroidCredentialResult {
        return repository.unlockWithAndroidCredential()
    }
}
