package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class ObserveVaultAndroidCredentialProtectedMaterialUseCase(
    private val repository: VaultRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.hasAndroidCredentialProtectedUnlockMaterial
    }
}
