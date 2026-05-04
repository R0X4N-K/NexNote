package com.example.nexnote.ui.screen.trash

import com.example.nexnote.data.db.NoteDao
import com.example.nexnote.data.db.entity.NoteEntity
import com.example.nexnote.data.db.model.NoteLinkCandidateProjection
import com.example.nexnote.domain.repository.NoteImageStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.InputStream

internal class FakeNoteImageStorage(
    private val events: MutableList<String>
) : NoteImageStorage {
    val deletedPaths = mutableListOf<String>()

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String = "images/note_${noteId}_img_0.jpg"

    override suspend fun deleteImage(relativePath: String): Boolean {
        deletedPaths += relativePath
        events += "image:$relativePath"
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
    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        MutableStateFlow(emptyList())
    override fun getAllCreationDates(): Flow<List<Long>> = MutableStateFlow(emptyList())

    override suspend fun getNoteById(id: Long): NoteEntity? = null
    override fun searchNotes(query: String): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())
    override fun getNotesByDateRange(
        startMs: Long,
        endMs: Long
    ): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())

    override suspend fun insertNote(note: NoteEntity): Long = 0L
    override suspend fun updateNote(note: NoteEntity) = Unit
    override suspend fun moveToTrash(id: Long, deletedDate: Long) = Unit
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
    override suspend fun updateNoteContent(id: Long, content: String, lastModifiedDate: Long) = Unit

    override suspend fun restoreFromTrash(id: Long) {
        restoredCount++
        lastRestoredId = id
    }

    override suspend fun deleteNotePermanently(id: Long) {
        permanentlyDeletedCount++
        events += "dao:delete:$id"
    }

    override suspend fun emptyTrash() {
        emptyTrashCount++
        events += "dao:empty"
    }
}
