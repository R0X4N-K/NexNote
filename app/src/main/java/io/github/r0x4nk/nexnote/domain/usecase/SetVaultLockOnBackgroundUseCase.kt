package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetVaultLockOnBackgroundUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(value: Boolean) {
        repository.setLockVaultOnBackground(value)
    }
}
