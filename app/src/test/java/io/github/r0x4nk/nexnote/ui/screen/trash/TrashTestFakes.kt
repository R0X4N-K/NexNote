package io.github.r0x4nk.nexnote.ui.screen.trash

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.InputStream

internal class FakeNoteImageStorage(
    private val events: MutableList<String>
) : NoteImageStorage {
    val deletedPaths = mutableListOf<String>()
    var deleteFailuresRemaining: Int = 0
    var deleteFailure: Throwable? = null

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String = "images/note_${noteId}_img_0.jpg"

    override suspend fun deleteImage(relativePath: String): Boolean {
        deletedPaths += relativePath
        events += "image:$relativePath"
        deleteFailure?.let { throw it }
        if (deleteFailuresRemaining > 0) {
            deleteFailuresRemaining--
            return false
        }
        return true
    }

    override fun getImageFile(relativePath: String): File = File(relativePath)
}

internal class FakeNoteDao(
    private val events: MutableList<String>
) : NoteDao {

    private val _allNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    private val _deletedNotes = MutableStateFlow<List<NoteEntity>>(emptyList())

    var permanentlyDeletedCount = 0
    var emptyTrashCount = 0
    var restoredCount = 0
    var lastRestoredId: Long? = null

    fun emitDeletedNotes(notes: List<NoteEntity>) {
        _deletedNotes.value = notes
    }

    override fun getAllNotes(): Flow<List<NoteEntity>> = _allNotes
    override fun getAllNotesSortedAsc(): Flow<List<NoteEntity>> = _allNotes
    override fun getDeletedNotes(): Flow<List<NoteEntity>> = _deletedNotes
    override fun getDeletedVaultNotes(): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())
    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        MutableStateFlow(emptyList())
    override fun getVaultNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        MutableStateFlow(emptyList())
    override fun getAllCreationDates(): Flow<List<Long>> = MutableStateFlow(emptyList())

    override suspend fun getNoteById(id: Long): NoteEntity? =
        (_allNotes.value + _deletedNotes.value).firstOrNull { it.id == id }
    override fun getAllVaultNotes(): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())
    override suspend fun getVaultNoteById(id: Long): NoteEntity? = null
    override suspend fun getAllVaultNotesForWipeOnce(): List<NoteEntity> = emptyList()
    override suspend fun getDeletedVaultNoteById(id: Long): NoteEntity? = null
    override suspend fun deleteAllVaultNotes(): Int = 0
    override fun searchNotes(query: String): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())
    override fun getNotesByDateRange(
        startMs: Long,
        endMs: Long
    ): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())

    override suspend fun insertNote(note: NoteEntity): Long = 0L
    override suspend fun updateNote(note: NoteEntity) = Unit
    override suspend fun moveToTrash(id: Long, deletedDate: Long) = Unit
    override suspend fun moveVaultNoteToTrash(id: Long, deletedDate: Long): Int = 0
    override suspend fun restoreVaultNoteFromTrash(id: Long): Int = 0
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
    override suspend fun restoreFromTrash(id: Long) {
        restoredCount++
        lastRestoredId = id
    }

    override suspend fun deleteNotePermanently(id: Long): Int {
        val existing = _deletedNotes.value.firstOrNull { it.id == id && it.isDeleted }
        if (existing == null) return 0
        _deletedNotes.value = _deletedNotes.value.filterNot { it.id == id }
        permanentlyDeletedCount++
        events += "dao:delete:$id"
        return 1
    }

    override suspend fun deleteVaultNotePermanently(id: Long): Int = 0

    override suspend fun emptyTrash(): Int {
        val deletedCount = _deletedNotes.value.count { it.isDeleted }
        _deletedNotes.value = _deletedNotes.value.filterNot { it.isDeleted }
        emptyTrashCount++
        events += "dao:empty"
        return deletedCount
    }

    override suspend fun getDeletedImagePathsRaw(): List<String> =
        _deletedNotes.value
            .filter { it.isDeleted && it.imagePathsRaw.isNotBlank() }
            .map { it.imagePathsRaw }
}
