package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveVaultAutoLockTimeoutUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<VaultAutoLockTimeout> {
        return repository.vaultAutoLockTimeout
    }
}
