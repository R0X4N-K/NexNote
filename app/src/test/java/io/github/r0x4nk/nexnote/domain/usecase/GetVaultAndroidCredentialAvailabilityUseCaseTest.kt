package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GetVaultAndroidCredentialAvailabilityUseCaseTest {

    @Test
    fun `returns repository availability without transformation`() {
        VaultAndroidCredentialAvailability.entries.forEach { availability ->
            val repository = FakeVaultAndroidCredentialRepository(availability)
            val useCase = GetVaultAndroidCredentialAvailabilityUseCase(repository)

            assertEquals(availability, useCase())
        }
    }
}

private class FakeVaultAndroidCredentialRepository(
    private val availability: VaultAndroidCredentialAvailability
) : VaultAndroidCredentialRepository {
    override fun getAvailability(): VaultAndroidCredentialAvailability {
        return availability
    }
}
