package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetVaultAutoLockTimeoutUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(timeout: VaultAutoLockTimeout) {
        repository.setVaultAutoLockTimeout(timeout)
    }
}
