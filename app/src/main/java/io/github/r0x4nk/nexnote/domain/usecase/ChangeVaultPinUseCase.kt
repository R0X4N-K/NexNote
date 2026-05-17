package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

class ChangeVaultPinUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult {
        return repository.changePin(currentPin, newPin)
    }
}
