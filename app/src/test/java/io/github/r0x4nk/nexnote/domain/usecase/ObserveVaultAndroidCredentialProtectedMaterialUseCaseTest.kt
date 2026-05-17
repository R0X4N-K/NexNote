package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveVaultAndroidCredentialProtectedMaterialUseCaseTest {

    @Test
    fun `invoke delegates protected material flow`() = runTest {
        val repository = FakeProtectedMaterialRepository()
        val useCase = ObserveVaultAndroidCredentialProtectedMaterialUseCase(repository)

        assertFalse(useCase().first())

        repository.setProtectedMaterialPresent(true)

        assertTrue(useCase().first())
    }
}

private class FakeProtectedMaterialRepository : VaultRepository {
    private val protectedMaterial = MutableStateFlow(false)

    override val state: Flow<VaultState> = MutableStateFlow(VaultState.LOCKED)
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        protectedMaterial

    fun setProtectedMaterialPresent(value: Boolean) {
        protectedMaterial.value = value
    }

    override suspend fun configurePin(pin: CharArray) = Unit

    override suspend fun unlockWithPin(pin: CharArray): Boolean = false

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult =
        RefreshVaultAndroidCredentialProtectedMaterialResult.Failed

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() = Unit

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult = ChangeVaultPinResult.RewrapFailed

    override suspend fun resetVault(): ResetVaultResult = ResetVaultResult.Failed

    override fun lock() = Unit
}
