package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

class LockVaultUseCase(
    private val repository: VaultRepository
) {
    operator fun invoke() {
        repository.lock()
    }
}
