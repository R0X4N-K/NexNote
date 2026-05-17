package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

class ClearVaultAndroidCredentialProtectedMaterialUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke() {
        repository.clearAndroidCredentialProtectedUnlockMaterial()
    }
}
