package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository

/**
 * Domain wrapper over [VaultRepository.resetVault].
 *
 * The use case is intentionally a thin pass-through: the destructive
 * semantics, the independence from the current Vault lock state and the
 * coherence guarantees on failure are all enforced by the repository
 * implementation. Keeping the use case minimal avoids duplicating those
 * guarantees and prevents this layer from inspecting or leaking PIN or
 * key material.
 *
 * No UI, navigation, preference toggle or user-facing action is added by
 * this wrapper: coordinating user preferences (e.g. disabling the Android
 * screen-lock unlock toggle) and the confirmation flow remains the
 * responsibility of subsequent steps that wire this use case into the
 * Settings layer.
 */
class ResetVaultUseCase(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(): ResetVaultResult {
        return repository.resetVault()
    }
}
