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

class UnlockVaultWithAndroidCredentialUseCaseTest {

    @Test
    fun `invoke delegates to repository and returns result`() = runTest {
        val repository = FakeAndroidCredentialUnlockRepository(
            result = UnlockVaultWithAndroidCredentialResult.AuthenticationRequired
        )
        val useCase = UnlockVaultWithAndroidCredentialUseCase(repository)

        val result = useCase()

        assertSame(UnlockVaultWithAndroidCredentialResult.AuthenticationRequired, result)
        assertEquals(1, repository.unlockWithAndroidCredentialCalls)
    }
}

private class FakeAndroidCredentialUnlockRepository(
    private val result: UnlockVaultWithAndroidCredentialResult
) : VaultRepository {
    override val state: Flow<VaultState> = MutableStateFlow(VaultState.LOCKED)
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        MutableStateFlow(true)

    var unlockWithAndroidCredentialCalls = 0
        private set

    override suspend fun configurePin(pin: CharArray) = Unit

    override suspend fun unlockWithPin(pin: CharArray): Boolean = false

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult {
        unlockWithAndroidCredentialCalls += 1
        return result
    }

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
