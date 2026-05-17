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
import org.junit.Assert.assertSame
import org.junit.Test

class ChangeVaultPinUseCaseTest {

    @Test
    fun `invoke delegates pins to repository and returns result`() = runTest {
        val repository = FakeChangeVaultPinRepository(
            result = ChangeVaultPinResult.Success
        )
        val useCase = ChangeVaultPinUseCase(repository)
        val currentPin = charArrayOf('1', '2', '3', '4')
        val newPin = charArrayOf('5', '6', '7', '8')

        val result = useCase(currentPin, newPin)

        assertSame(ChangeVaultPinResult.Success, result)
        assertEquals(1, repository.changePinCalls)
        assertSame(currentPin, repository.lastCurrentPin)
        assertSame(newPin, repository.lastNewPin)
    }
}

private class FakeChangeVaultPinRepository(
    private val result: ChangeVaultPinResult
) : VaultRepository {
    override val state: Flow<VaultState> = MutableStateFlow(VaultState.UNLOCKED)
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        MutableStateFlow(false)

    var changePinCalls = 0
        private set
    var lastCurrentPin: CharArray? = null
        private set
    var lastNewPin: CharArray? = null
        private set

    override suspend fun configurePin(pin: CharArray) = Unit

    override suspend fun unlockWithPin(pin: CharArray): Boolean = true

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult =
        RefreshVaultAndroidCredentialProtectedMaterialResult.Failed

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() = Unit

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult {
        changePinCalls++
        lastCurrentPin = currentPin
        lastNewPin = newPin
        return result
    }

    override suspend fun resetVault(): ResetVaultResult = ResetVaultResult.Failed

    override fun lock() = Unit
}
