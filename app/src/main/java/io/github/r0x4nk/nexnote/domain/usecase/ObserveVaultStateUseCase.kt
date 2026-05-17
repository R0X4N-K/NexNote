package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class ObserveVaultStateUseCase(
    private val repository: VaultRepository
) {
    operator fun invoke(): Flow<VaultState> {
        return repository.state
    }
}
