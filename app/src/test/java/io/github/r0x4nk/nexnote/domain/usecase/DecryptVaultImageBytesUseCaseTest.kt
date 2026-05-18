package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DecryptVaultImageBytesUseCaseTest {

    @Test
    fun `invoke delegates to repository and returns decrypted bytes`() = runTest {
        val expected = byteArrayOf(0x42, 0x4D, 0x50)
        val repo = FakeDecryptRepository(decryptResult = expected)
        val useCase = DecryptVaultImageBytesUseCase(repo)

        val result = useCase("images/vault_1.jpg")

        assertArrayEquals(expected, result)
        assertEquals(1, repo.decryptCalls)
        assertEquals("images/vault_1.jpg", repo.lastPath)
    }

    @Test
    fun `invoke returns null when repository returns null`() = runTest {
        val repo = FakeDecryptRepository(decryptResult = null)
        val useCase = DecryptVaultImageBytesUseCase(repo)

        val result = useCase("images/missing.jpg")

        assertNull(result)
        assertEquals(1, repo.decryptCalls)
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        val repo = FakeDecryptRepository(shouldThrow = true)
        val useCase = DecryptVaultImageBytesUseCase(repo)

        val error = runCatching { useCase("images/vault_1.jpg") }.exceptionOrNull()

        assertSame(repo.thrownException::class, error!!::class)
        assertEquals(1, repo.decryptCalls)
    }
}

private class FakeDecryptRepository(
    private val decryptResult: ByteArray? = null,
    private val shouldThrow: Boolean = false
) : VaultNoteRepository {
    var decryptCalls = 0
    var lastPath: String? = null
    val thrownException = RuntimeException("decrypt failed")

    override val vaultNotes: Flow<List<Note>> = flowOf(emptyList())

    override suspend fun getVaultNoteById(id: Long): Note? = null
    override suspend fun saveVaultNote(note: Note): Long = 0L
    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult =
        MoveNoteToVaultResult.NotFound
    override suspend fun removeNoteFromVault(id: Long): Boolean = false
    override suspend fun moveVaultNoteToTrash(id: Long): Boolean = false

    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? {
        decryptCalls++
        lastPath = relativePath
        if (shouldThrow) throw thrownException
        return decryptResult
    }
}
