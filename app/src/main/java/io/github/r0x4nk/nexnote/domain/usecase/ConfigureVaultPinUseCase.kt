package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

class ConfigureVaultPinUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(pin: CharArray) {
        repository.configurePin(pin)
    }
}
