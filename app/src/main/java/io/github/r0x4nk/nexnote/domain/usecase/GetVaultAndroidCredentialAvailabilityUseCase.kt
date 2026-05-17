package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository

class GetVaultAndroidCredentialAvailabilityUseCase(
    private val repository: VaultAndroidCredentialRepository
) {
    operator fun invoke(): VaultAndroidCredentialAvailability {
        return repository.getAvailability()
    }
}
