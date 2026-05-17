package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability

interface VaultAndroidCredentialRepository {
    fun getAvailability(): VaultAndroidCredentialAvailability
}
