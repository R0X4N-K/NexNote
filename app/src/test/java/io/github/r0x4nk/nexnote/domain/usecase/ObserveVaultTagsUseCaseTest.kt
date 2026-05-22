package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveVaultTagsUseCaseTest {

    @Test
    fun `invoke pass-through emits repository tags when vault is unlocked`() = runTest {
        val expected = listOf(
            Tag(name = "alpha", noteCount = 3, createdDate = 10L, lastUpdatedDate = 50L),
            Tag(name = "beta", noteCount = 1, createdDate = 20L, lastUpdatedDate = 30L)
        )
        val repo = FakeTagsVaultNoteRepository(MutableStateFlow(expected))
        val useCase = ObserveVaultTagsUseCase(repo)

        val emitted = useCase().first()

        assertEquals(expected, emitted)
    }

    @Test
    fun `invoke emits empty list when repository indicates locked vault`() = runTest {
        val repo = FakeTagsVaultNoteRepository(MutableStateFlow(emptyList()))
        val useCase = ObserveVaultTagsUseCase(repo)

        val emitted = useCase().first()

        assertTrue(emitted.isEmpty())
    }
}

private class FakeTagsVaultNoteRepository(
    private val tagsFlow: MutableStateFlow<List<Tag>>
) : VaultNoteRepository {
    override val vaultNotes: Flow<List<Note>> = flowOf(emptyList())
    override val vaultTrashedNotes: Flow<List<Note>> = flowOf(emptyList())
    override val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>> = flowOf(emptyList())
    override val vaultTags: Flow<List<Tag>> = tagsFlow

    override suspend fun getVaultNoteById(id: Long): Note? = null
    override suspend fun saveVaultNote(note: Note): Long = 0L
    override suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult =
        DuplicateVaultNoteResult.NotFound
    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult =
        MoveNoteToVaultResult.NotFound
    override suspend fun removeNoteFromVault(id: Long): Boolean = false
    override suspend fun moveVaultNoteToTrash(id: Long): Boolean = false
    override suspend fun restoreVaultNoteFromTrash(id: Long): Boolean = false
    override suspend fun deleteVaultNotePermanently(id: Long): Boolean = false
    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? = null
}
