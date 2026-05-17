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

class ResetVaultUseCaseTest {

    @Test
    fun `invoke delegates to repository and returns Success result`() = runTest {
        val repository = FakeResetVaultRepository(result = ResetVaultResult.Success)
        val useCase = ResetVaultUseCase(repository)

        val result = useCase()

        assertSame(ResetVaultResult.Success, result)
        assertEquals(1, repository.resetVaultCalls)
    }

    @Test
    fun `invoke propagates VaultNotConfigured result without extra calls`() = runTest {
        val repository = FakeResetVaultRepository(result = ResetVaultResult.VaultNotConfigured)
        val useCase = ResetVaultUseCase(repository)

        val result = useCase()

        assertSame(ResetVaultResult.VaultNotConfigured, result)
        assertEquals(1, repository.resetVaultCalls)
    }

    @Test
    fun `invoke propagates VaultLocked result without extra calls`() = runTest {
        val repository = FakeResetVaultRepository(result = ResetVaultResult.VaultLocked)
        val useCase = ResetVaultUseCase(repository)

        val result = useCase()

        assertSame(ResetVaultResult.VaultLocked, result)
        assertEquals(1, repository.resetVaultCalls)
    }

    @Test
    fun `invoke propagates Failed result without extra calls`() = runTest {
        val repository = FakeResetVaultRepository(result = ResetVaultResult.Failed)
        val useCase = ResetVaultUseCase(repository)

        val result = useCase()

        assertSame(ResetVaultResult.Failed, result)
        assertEquals(1, repository.resetVaultCalls)
    }
}

private class FakeResetVaultRepository(
    private val result: ResetVaultResult
) : VaultRepository {
    override val state: Flow<VaultState> = MutableStateFlow(VaultState.NOT_CONFIGURED)
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        MutableStateFlow(false)

    var resetVaultCalls = 0
        private set

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

    override suspend fun resetVault(): ResetVaultResult {
        resetVaultCalls++
        return result
    }

    override fun lock() = Unit
}
