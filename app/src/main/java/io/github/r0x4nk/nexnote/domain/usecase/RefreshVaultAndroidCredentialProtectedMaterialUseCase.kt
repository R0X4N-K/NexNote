package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

class RefreshVaultAndroidCredentialProtectedMaterialUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(): RefreshVaultAndroidCredentialProtectedMaterialResult {
        return repository.refreshAndroidCredentialProtectedUnlockMaterial()
    }
}
