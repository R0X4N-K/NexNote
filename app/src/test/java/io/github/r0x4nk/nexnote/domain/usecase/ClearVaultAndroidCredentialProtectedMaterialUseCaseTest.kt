package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearVaultAndroidCredentialProtectedMaterialUseCaseTest {

    @Test
    fun `delegates to repository`() = runTest {
        val repository = FakeVaultRepository()
        val useCase = ClearVaultAndroidCredentialProtectedMaterialUseCase(repository)

        useCase()

        assertEquals(1, repository.clearCalls)
    }
}

private class FakeVaultRepository : VaultRepository {
    override val state: Flow<VaultState> = MutableStateFlow(VaultState.LOCKED)
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        MutableStateFlow(true)
    var clearCalls: Int = 0
        private set

    override suspend fun configurePin(pin: CharArray) = Unit
    override suspend fun unlockWithPin(pin: CharArray): Boolean = false
    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult =
            RefreshVaultAndroidCredentialProtectedMaterialResult.Failed

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() {
        clearCalls += 1
    }

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult = ChangeVaultPinResult.VaultLocked

    override suspend fun resetVault(): ResetVaultResult = ResetVaultResult.Failed

    override fun lock() = Unit
}
